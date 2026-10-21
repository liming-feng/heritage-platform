package com.lmf.heritage.module.log.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.lmf.heritage.common.Constants;
import com.lmf.heritage.common.PageResult;
import com.lmf.heritage.module.log.dto.OperationLogQueryDTO;
import com.lmf.heritage.module.log.entity.OperationLog;
import com.lmf.heritage.module.log.mapper.OperationLogMapper;
import com.lmf.heritage.module.log.service.OperationLogService;
import com.lmf.heritage.module.log.vo.OperationLogExcelVO;
import com.lmf.heritage.module.log.vo.OperationLogVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * 操作日志服务实现
 *
 * @author 李明峰 23100025
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class OperationLogServiceImpl extends ServiceImpl<OperationLogMapper, OperationLog>
        implements OperationLogService {

    private static final DateTimeFormatter DT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    /** 导出条数上限：日志表增长最快，不设上限的话一次导出能拖垮内存与响应 */
    private static final int EXPORT_LIMIT = 5000;

    /**
     * 异步入库
     *
     * <p><b>为什么这里必须 try-catch 兜住所有异常</b>：日志是「旁路」能力，
     * 它失败绝不能影响业务。异步线程里抛出的异常虽然不会回传到请求线程，
     * 但会被 {@code AsyncConfig} 的 uncaughtHandler 记成 error，把真实错误淹掉；
     * 而写日志失败本身往往意味着库出了问题，此处只记 warn、不重试——
     * 让日志模块永远不要成为新的故障源。</p>
     */
    @Override
    @Async("taskExecutor")
    public void saveAsync(OperationLog entity) {
        try {
            this.save(entity);
        } catch (Exception e) {
            log.warn("[操作日志] 入库失败，已忽略：{} - {}", entity == null ? null : entity.getOperation(),
                    e.getMessage());
        }
    }

    @Override
    public PageResult<OperationLogVO> page(OperationLogQueryDTO query) {
        query.normalize();
        Page<OperationLog> page = new Page<>(query.getPageNum(), query.getPageSize());
        Page<OperationLog> result = this.page(page, buildWrapper(query));
        List<OperationLogVO> records = result.getRecords().stream().map(this::toVO).toList();
        return PageResult.of(result, records);
    }

    @Override
    public List<OperationLogExcelVO> exportList(OperationLogQueryDTO query) {
        query.normalize();
        // 导出与列表共用同一个 buildWrapper —— 保证「导出的就是筛选后的那一批」
        List<OperationLog> list = this.list(buildWrapper(query).last("LIMIT " + EXPORT_LIMIT));
        return list.stream().map(this::toExcelVO).toList();
    }

    @Override
    public List<String> operationTypes() {
        LambdaQueryWrapper<OperationLog> wrapper = new LambdaQueryWrapper<OperationLog>()
                .select(OperationLog::getOperation)
                .isNotNull(OperationLog::getOperation)
                .groupBy(OperationLog::getOperation)
                .orderByAsc(OperationLog::getOperation);
        return this.listObjs(wrapper, o -> o == null ? null : o.toString())
                .stream().filter(s -> s != null && !s.isEmpty()).toList();
    }

    /* ==================== 内部工具 ==================== */

    /**
     * 构造筛选条件（列表与导出共用）
     *
     * <p>排序固定为 {@code create_time DESC, id DESC}：日志在同一毫秒内可能有多条，
     * 只按时间排序会出现「同秒内顺序随机」，补上 id 才能得到稳定且符合直觉的倒序。</p>
     */
    private LambdaQueryWrapper<OperationLog> buildWrapper(OperationLogQueryDTO q) {
        return new LambdaQueryWrapper<OperationLog>()
                .like(q.getOperator() != null, OperationLog::getOperator, q.getOperator())
                .like(q.getOperation() != null, OperationLog::getOperation, q.getOperation())
                .eq(q.getRole() != null, OperationLog::getRole, q.getRole())
                .eq(q.getSuccess() != null, OperationLog::getSuccess, q.getSuccess())
                .ge(q.getStartTime() != null, OperationLog::getCreateTime, q.getStartTime())
                .le(q.getEndTime() != null, OperationLog::getCreateTime, q.getEndTime())
                .orderByDesc(OperationLog::getCreateTime)
                .orderByDesc(OperationLog::getId);
    }

    private OperationLogVO toVO(OperationLog e) {
        OperationLogVO vo = new OperationLogVO();
        vo.setId(e.getId());
        vo.setOperatorId(e.getOperatorId());
        vo.setOperator(e.getOperator());
        vo.setRole(e.getRole());
        vo.setRoleText(roleText(e.getRole()));
        vo.setOperation(e.getOperation());
        vo.setMethod(e.getMethod());
        vo.setParams(e.getParams());
        vo.setIp(e.getIp());
        vo.setCostMs(e.getCostMs());
        vo.setCostText(e.getCostMs() == null ? null : e.getCostMs() + " ms");
        vo.setSuccess(e.getSuccess());
        vo.setSuccessText(Integer.valueOf(1).equals(e.getSuccess()) ? "成功" : "失败");
        vo.setErrorMsg(e.getErrorMsg());
        vo.setCreateTime(e.getCreateTime());
        return vo;
    }

    private OperationLogExcelVO toExcelVO(OperationLog e) {
        OperationLogExcelVO vo = new OperationLogExcelVO();
        vo.setCreateTime(e.getCreateTime() == null ? null : e.getCreateTime().format(DT));
        vo.setOperator(e.getOperator());
        vo.setRoleText(roleText(e.getRole()));
        vo.setOperation(e.getOperation());
        vo.setMethod(e.getMethod());
        vo.setIp(e.getIp());
        vo.setCostMs(e.getCostMs());
        vo.setSuccessText(Integer.valueOf(1).equals(e.getSuccess()) ? "成功" : "失败");
        vo.setErrorMsg(e.getErrorMsg());
        vo.setParams(e.getParams());
        return vo;
    }

    private static String roleText(Integer role) {
        if (role == null) {
            return "游客";
        }
        return switch (role) {
            case Constants.ROLE_USER -> "用户";
            case Constants.ROLE_CRAFTSMAN -> "匠人";
            case Constants.ROLE_ADMIN -> "管理员";
            default -> "未知";
        };
    }
}
