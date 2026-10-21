package com.lmf.heritage.aspect;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.lmf.heritage.annotation.OperationLog;
import com.lmf.heritage.module.log.service.OperationLogService;
import com.lmf.heritage.security.UserContext;
import com.lmf.heritage.util.IpUtil;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.stereotype.Component;
import org.springframework.validation.BindingResult;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

/**
 * 操作日志切面
 *
 * <p>对标注了 {@link OperationLog} 的方法做环绕增强，采集「谁、做了什么、结果如何、
 * 来自哪个 IP、花了多久」，最后交给 {@code OperationLogService.saveAsync} 异步入库。</p>
 *
 * <p><b>为什么是环绕而不是前置 + 后置</b>：耗时只有环绕能测准（前/后置两次增强各自
 * 取一次时间，中间还夹着框架自己的开销）；而「成功还是失败」必须在同一个点上才能
 * 和耗时对齐——分成两块写，会出现「耗时记了、结果因为异常没记上」的半条日志。</p>
 *
 * <p><b>为什么操作人要在请求线程里取</b>：{@code UserContext} 是 ThreadLocal，
 * 业务方法返回后请求线程会被回收，异步线程里读到的只会是 null。所以这里先把
 * 操作人「快照」成一个普通对象再交出去——这也是日志里存用户名副本而不是只存
 * 外键的原因之一。</p>
 *
 * <p><b>为什么参数要脱敏</b>：登录、改密这类接口的参数里带着明文密码。
 * 日志表是长期保留、且管理员可查的，把密码原样写进去等于制造一个稳定的泄漏点。</p>
 *
 * @author 李明峰 23100025
 */
@Slf4j
@Aspect
@Component
@RequiredArgsConstructor
public class OperationLogAspect {

    /** 与 operation_log.params 的列宽一致 */
    private static final int PARAMS_MAX = 2000;

    /** 与 operation_log.error_msg 的列宽一致 */
    private static final int ERROR_MAX = 500;

    /**
     * 敏感字段脱敏
     *
     * <p>匹配 JSON 里形如 {@code "password":"xxx"} / {@code "oldPassword":"xxx"} /
     * {@code "accessToken":"xxx"} 的键值对，只保留键名、抹掉值。
     * 用正则处理序列化后的文本，而不是逐个 DTO 加 {@code @JsonIgnore}——
     * 后者要求每个新增的入参类都记得加，而漏加的代价是真实泄漏。</p>
     */
    private static final Pattern SENSITIVE = Pattern.compile(
            "(\"[A-Za-z0-9_]*(?:password|pwd|token|secret)[A-Za-z0-9_]*\"\\s*:\\s*\")[^\"]*(\")",
            Pattern.CASE_INSENSITIVE);

    private final OperationLogService operationLogService;
    private final ObjectMapper objectMapper;

    @Around("@annotation(operationLog)")
    public Object around(ProceedingJoinPoint pjp, OperationLog operationLog) throws Throwable {
        long start = System.currentTimeMillis();

        // —— 以下信息必须在请求线程里取齐，异步线程拿不到 ——
        UserContext.LoginUser login = UserContext.get();
        HttpServletRequest request = currentRequest();
        String ip = IpUtil.getIp(request);
        String method = pjp.getSignature().getDeclaringType().getSimpleName()
                + "." + pjp.getSignature().getName();
        String params = operationLog.saveParams() ? buildParams(pjp.getArgs()) : null;

        boolean success = true;
        String errorMsg = null;
        try {
            return pjp.proceed();
        } catch (Throwable t) {
            success = false;
            errorMsg = truncate(rootMessage(t), ERROR_MAX);
            throw t;
        } finally {
            // 注意：这里刻意用全限定名 —— 本类顶部 import 的 OperationLog 是注解，
            // 与 module.log.entity.OperationLog 同名，只能有一者用短名。
            com.lmf.heritage.module.log.entity.OperationLog entity =
                    new com.lmf.heritage.module.log.entity.OperationLog();
            entity.setOperatorId(login == null ? null : login.getUserId());
            entity.setOperator(login == null ? null : login.getUsername());
            entity.setRole(login == null ? null : login.getRole());
            entity.setOperation(operationLog.value());
            entity.setMethod(method);
            entity.setParams(params);
            entity.setIp(ip);
            entity.setCostMs(System.currentTimeMillis() - start);
            entity.setSuccess(success ? 1 : 0);
            entity.setErrorMsg(errorMsg);
            operationLogService.saveAsync(entity);
        }
    }

    /* ==================== 内部工具 ==================== */

    private static HttpServletRequest currentRequest() {
        ServletRequestAttributes attrs =
                (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        return attrs == null ? null : attrs.getRequest();
    }

    /**
     * 把方法入参序列化成一行 JSON
     *
     * <p>Servlet 的请求/响应对象、上传的文件流、参数校验结果这几类要么不可序列化、
     * 要么内容巨大且无意义，统一跳过；全部被跳过时返回 null 而不是 {@code "[]"}，
     * 让日志表里的「无参数」和「参数是空数组」区分得开。</p>
     */
    private String buildParams(Object[] args) {
        if (args == null || args.length == 0) {
            return null;
        }
        List<Object> kept = new ArrayList<>(args.length);
        for (Object arg : args) {
            if (arg == null) {
                kept.add(null);
                continue;
            }
            if (arg instanceof HttpServletRequest || arg instanceof HttpServletResponse
                    || arg instanceof MultipartFile || arg instanceof MultipartFile[]
                    || arg instanceof InputStream || arg instanceof OutputStream
                    || arg instanceof BindingResult) {
                continue;
            }
            kept.add(arg);
        }
        if (kept.isEmpty()) {
            return null;
        }
        String json;
        try {
            json = objectMapper.writeValueAsString(kept);
        } catch (Exception e) {
            // 序列化失败不该让业务失败：退化成一条说明性文本
            return "[参数序列化失败: " + e.getClass().getSimpleName() + "]";
        }
        return truncate(SENSITIVE.matcher(json).replaceAll("$1***$2"), PARAMS_MAX);
    }

    /** 取最内层异常的 message —— 业务异常通常包了一层，外层信息量很低 */
    private static String rootMessage(Throwable t) {
        Throwable cur = t;
        while (cur.getCause() != null && cur.getCause() != cur) {
            cur = cur.getCause();
        }
        String msg = cur.getMessage();
        if (msg == null || msg.isBlank()) {
            msg = cur.getClass().getSimpleName();
        }
        return msg;
    }

    /**
     * 按列宽截断
     *
     * <p>不截断的话，超长参数会让整条 INSERT 直接失败（MySQL 严格模式报
     * {@code Data too long}），而那是一条日志——为了它把业务事务拖下水不划算。</p>
     */
    private static String truncate(String s, int max) {
        if (s == null) {
            return null;
        }
        return s.length() <= max ? s : s.substring(0, max - 3) + "...";
    }
}
