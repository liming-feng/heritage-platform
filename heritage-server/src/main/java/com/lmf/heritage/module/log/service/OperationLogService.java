package com.lmf.heritage.module.log.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.lmf.heritage.common.PageResult;
import com.lmf.heritage.module.log.dto.OperationLogQueryDTO;
import com.lmf.heritage.module.log.entity.OperationLog;
import com.lmf.heritage.module.log.vo.OperationLogExcelVO;
import com.lmf.heritage.module.log.vo.OperationLogVO;

import java.util.List;

/**
 * 操作日志服务
 *
 * @author 李明峰 23100025
 */
public interface OperationLogService extends IService<OperationLog> {

    /**
     * 异步写入一条操作日志
     *
     * <p>由 {@code OperationLogAspect} 在请求线程里组装好实体后调用，
     * 真正的 INSERT 跑在 {@code taskExecutor} 线程池里——写日志的耗时
     * 不该计入用户请求的响应时间。</p>
     */
    void saveAsync(OperationLog entity);

    /** 日志列表（按操作人 / 操作类型 / 角色 / 结果 / 时间范围筛选） */
    PageResult<OperationLogVO> page(OperationLogQueryDTO query);

    /** 导出用：同一套筛选条件下不分页，内部有行数上限保护 */
    List<OperationLogExcelVO> exportList(OperationLogQueryDTO query);

    /** 日志里出现过的操作类型清单（供前端筛选下拉框使用） */
    List<String> operationTypes();
}
