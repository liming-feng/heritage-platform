package com.lmf.heritage.module.log.controller;

import com.alibaba.excel.EasyExcel;
import com.lmf.heritage.annotation.LoginRequired;
import com.lmf.heritage.common.Constants;
import com.lmf.heritage.common.PageResult;
import com.lmf.heritage.common.Result;
import com.lmf.heritage.module.log.dto.OperationLogQueryDTO;
import com.lmf.heritage.module.log.service.OperationLogService;
import com.lmf.heritage.module.log.vo.OperationLogExcelVO;
import com.lmf.heritage.module.log.vo.OperationLogVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.List;

/**
 * 管理端 · 操作日志
 *
 * <p>全站只读。日志由 {@code OperationLogAspect} 自动产生，不提供任何写入/修改接口——
 * 一条能被改写的审计日志没有意义。</p>
 *
 * @author 李明峰 23100025
 */
@Slf4j
@Tag(name = "管理端-操作日志", description = "审计日志查询与导出")
@RestController
@RequestMapping("/admin/log")
@RequiredArgsConstructor
@LoginRequired(roles = {Constants.ROLE_ADMIN})
public class AdminLogController {

    private final OperationLogService operationLogService;

    @Operation(summary = "操作日志列表",
            description = "支持按操作人（模糊）、操作类型（模糊）、角色、结果、时间范围筛选；"
                    + "固定按操作时间倒序")
    @GetMapping("/list")
    public Result<PageResult<OperationLogVO>> list(OperationLogQueryDTO query) {
        return Result.success(operationLogService.page(query));
    }

    @Operation(summary = "操作类型清单",
            description = "返回日志里实际出现过的操作描述，供前端筛选下拉框使用（避免前端硬编码一份会过期的列表）")
    @GetMapping("/types")
    public Result<List<String>> types() {
        return Result.success(operationLogService.operationTypes());
    }

    @Operation(summary = "导出操作日志 Excel",
            description = "筛选条件与列表接口一致，最多 5000 条")
    @GetMapping("/export")
    public void export(OperationLogQueryDTO query, HttpServletResponse response) throws IOException {
        List<OperationLogExcelVO> rows = operationLogService.exportList(query);

        response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        String fileName = URLEncoder.encode("操作日志", StandardCharsets.UTF_8).replace("+", "%20");
        response.setHeader("Content-Disposition", "attachment;filename*=utf-8''" + fileName + ".xlsx");

        EasyExcel.write(response.getOutputStream(), OperationLogExcelVO.class)
                .sheet("操作日志")
                .doWrite(rows);
        log.info("[操作日志] 导出 {} 条", rows.size());
    }
}
