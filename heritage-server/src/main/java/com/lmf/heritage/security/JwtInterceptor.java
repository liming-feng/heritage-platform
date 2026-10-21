package com.lmf.heritage.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.lmf.heritage.annotation.LoginRequired;
import com.lmf.heritage.common.Constants;
import com.lmf.heritage.common.ErrorCode;
import com.lmf.heritage.common.Result;
import io.jsonwebtoken.Claims;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerInterceptor;

import java.nio.charset.StandardCharsets;
import java.util.Arrays;

/**
 * JWT 鉴权拦截器
 *
 * <pre>
 * 职责：
 *  1. 解析 Authorization 头中的 access_token，写入 UserContext
 *  2. 校验 Redis 黑名单（退出登录后原 token 立即失效）
 *  3. 对带 &#64;LoginRequired 的接口强制登录，并校验 roles 角色白名单
 *  4. access_token 过期返回 HTTP 401（前端据此触发 refresh 换发）
 *  5. refresh_token 过期 / 非法返回 HTTP 403（前端跳登录页）
 * </pre>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class JwtInterceptor implements HandlerInterceptor {

    /** 认证类接口白名单（路径已不含 context-path） */
    private static final java.util.List<String> AUTH_APIS = java.util.List.of(
            "/portal/auth/login", "/portal/auth/register", "/portal/auth/refresh");

    private final JwtUtil jwtUtil;
    private final StringRedisTemplate redisTemplate;
    private final ObjectMapper objectMapper;
    private final com.lmf.heritage.util.UserStatusHelper userStatusHelper;

    @Value("${heritage.jwt.header:Authorization}")
    private String header;

    @Value("${heritage.jwt.prefix:Bearer }")
    private String prefix;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        // 放行预检请求
        if ("OPTIONS".equalsIgnoreCase(request.getMethod())) {
            return true;
        }
        // 去掉 context-path（/api）后的真实路径，用于前缀级角色隔离
        String uri = request.getRequestURI();
        String ctx = request.getContextPath();
        String path = ctx != null && !ctx.isEmpty() && uri.startsWith(ctx) ? uri.substring(ctx.length()) : uri;

        // 静态资源、未匹配到 Controller 的请求，handler 不是 HandlerMethod：
        // 这类请求没有注解可读，但「/admin/** 只有管理员能进」这条必须照常生效，
        // 否则普通用户访问一个还不存在的管理端地址就会绕过 403。
        // 因此这里不提前 return，只把 handlerMethod 置空。
        HandlerMethod handlerMethod = (handler instanceof HandlerMethod hm) ? hm : null;

        String token = JwtUtil.stripPrefix(request.getHeader(header), prefix);

        // 认证类接口（注册/登录/刷新）直接放行。
        // 原因：前端 axios 会给所有请求带上 Authorization 头，
        // 而 access_token 过期后用户重新登录时，这个过期 token 会被下面的
        // 校验判为 401，导致「过期后连登录都登不了」的死锁。
        if (isAuthApi(path)) {
            if (token != null && !token.isEmpty()) {
                try {
                    Claims c = jwtUtil.parse(token);
                    if (JwtUtil.TYPE_ACCESS.equals(jwtUtil.getType(c))) {
                        UserContext.set(new UserContext.LoginUser(
                                jwtUtil.getUserId(c), jwtUtil.getUsername(c), jwtUtil.getRole(c)));
                    }
                } catch (com.lmf.heritage.common.BusinessException ignored) {
                    // 认证接口不因 token 失效而拦截
                }
            }
            return true;
        }

        if (token == null || token.isEmpty()) {
            // 未携带 token：接口要求登录、或访问的是匠人端/管理端前缀，一律 401
            if ((handlerMethod != null && requiresLogin(handlerMethod)) || isProtectedPrefix(path)) {
                writeJson(response, HttpServletResponse.SC_UNAUTHORIZED, ErrorCode.NOT_LOGIN);
                return false;
            }
            return true;
        }

        Claims claims;
        try {
            claims = jwtUtil.parse(token);
        } catch (com.lmf.heritage.common.BusinessException e) {
            // 令牌非法直接 401；已过期也 401（让前端尝试 refresh）
            writeJson(response, HttpServletResponse.SC_UNAUTHORIZED, e.getMessage());
            return false;
        }

        // 黑名单校验（退出登录后原 token 立即失效）
        Boolean blacklisted = redisTemplate.hasKey(Constants.KEY_TOKEN_BLACKLIST + token);
        if (Boolean.TRUE.equals(blacklisted)) {
            writeJson(response, HttpServletResponse.SC_UNAUTHORIZED, ErrorCode.TOKEN_INVALID.getMessage());
            return false;
        }

        // 只接受 access_token 访问业务接口
        if (!JwtUtil.TYPE_ACCESS.equals(jwtUtil.getType(claims))) {
            writeJson(response, HttpServletResponse.SC_UNAUTHORIZED, ErrorCode.TOKEN_INVALID.getMessage());
            return false;
        }

        UserContext.LoginUser loginUser = new UserContext.LoginUser(
                jwtUtil.getUserId(claims), jwtUtil.getUsername(claims), jwtUtil.getRole(claims));
        UserContext.set(loginUser);

        // 账号启停校验：管理端禁用后，已签发的令牌必须立刻失效。
        // 放在角色校验之前——「账号被禁用」比「角色不够」是更根本的拒绝理由，
        // 先告诉用户被禁用，他才知道该去找管理员而不是怀疑权限配置。
        // 状态带 Redis 缓存（见 UserStatusHelper），正常请求几乎不查库。
        if (userStatusHelper.isDisabled(loginUser.getUserId())) {
            writeJson(response, HttpServletResponse.SC_FORBIDDEN, ErrorCode.USER_DISABLED);
            return false;
        }

        // 注解级角色隔离（@LoginRequired(roles = {...})）
        LoginRequired anno = handlerMethod == null ? null : findAnnotation(handlerMethod);
        if (anno != null && anno.roles().length > 0) {
            boolean allowed = Arrays.stream(anno.roles())
                    .anyMatch(r -> r == loginUser.getRole());
            if (!allowed) {
                writeJson(response, HttpServletResponse.SC_FORBIDDEN, ErrorCode.NO_PERMISSION.getMessage());
                return false;
            }
        }

        // 三端前缀级隔离：/admin/** 仅管理员，/craftsman/** 仅匠人（管理员放行以便排查）
        if (isUnder(path, "/admin") && !Integer.valueOf(Constants.ROLE_ADMIN).equals(loginUser.getRole())) {
            writeJson(response, HttpServletResponse.SC_FORBIDDEN, ErrorCode.NO_PERMISSION.getMessage());
            return false;
        }
        if (isUnder(path, "/craftsman")
                && !Integer.valueOf(Constants.ROLE_CRAFTSMAN).equals(loginUser.getRole())
                && !Integer.valueOf(Constants.ROLE_ADMIN).equals(loginUser.getRole())) {
            writeJson(response, HttpServletResponse.SC_FORBIDDEN, ErrorCode.NO_PERMISSION.getMessage());
            return false;
        }

        return true;
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) {
        // 必须清理，避免线程复用导致的用户串号
        UserContext.clear();
    }

    private boolean requiresLogin(HandlerMethod handlerMethod) {
        return findAnnotation(handlerMethod) != null;
    }

    /** 认证类接口：不需要登录态，也不因携带失效 token 而被拦截 */
    private boolean isAuthApi(String path) {
        return AUTH_APIS.stream().anyMatch(path::startsWith);
    }

    /** 匠人端 / 管理端前缀：未登录一律 401，不暴露接口是否存在 */
    private boolean isProtectedPrefix(String path) {
        return isUnder(path, "/admin") || isUnder(path, "/craftsman");
    }

    /** 精确的前缀判断：/admin 与 /admin/xxx 命中，/administrator 不命中 */
    private boolean isUnder(String path, String prefix) {
        return path.equals(prefix) || path.startsWith(prefix + "/");
    }

    private LoginRequired findAnnotation(HandlerMethod handlerMethod) {
        LoginRequired anno = handlerMethod.getMethodAnnotation(LoginRequired.class);
        if (anno == null) {
            anno = handlerMethod.getBeanType().getAnnotation(LoginRequired.class);
        }
        return anno;
    }

    /** 直接用业务错误码输出（业务码可能与 HTTP 码不一一对应，如 403 配 2004「账号已禁用」） */
    private void writeJson(HttpServletResponse response, int httpStatus, ErrorCode errorCode) throws Exception {
        writeJson(response, httpStatus, errorCode.getCode(), errorCode.getMessage());
    }

    /** 只有一条 message、业务码按 HTTP 状态推断（401 → 未登录，403 → 无权限） */
    private void writeJson(HttpServletResponse response, int httpStatus, String message) throws Exception {
        writeJson(response, httpStatus, bizCodeOf(httpStatus), message);
    }

    private void writeJson(HttpServletResponse response, int httpStatus, int bizCode, String message)
            throws Exception {
        response.setStatus(httpStatus);
        response.setContentType("application/json;charset=UTF-8");
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        Result<Void> body = Result.error(bizCode, message);
        response.getWriter().write(objectMapper.writeValueAsString(body));
    }

    private int bizCodeOf(int httpStatus) {
        return httpStatus == HttpServletResponse.SC_FORBIDDEN
                ? ErrorCode.NO_PERMISSION.getCode()
                : ErrorCode.NOT_LOGIN.getCode();
    }
}
