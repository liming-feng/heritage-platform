package com.lmf.heritage.module.craftsman.controller;

import com.alibaba.excel.EasyExcel;
import com.lmf.heritage.annotation.LoginRequired;
import com.lmf.heritage.annotation.OperationLog;
import com.lmf.heritage.common.Constants;
import com.lmf.heritage.common.PageResult;
import com.lmf.heritage.common.Result;
import com.lmf.heritage.module.craftsman.dto.AdminCraftsmanQueryDTO;
import com.lmf.heritage.module.craftsman.dto.CraftsmanAuditDTO;
import com.lmf.heritage.module.craftsman.service.AdminCraftsmanService;
import com.lmf.heritage.module.craftsman.vo.AdminCraftsmanVO;
import com.lmf.heritage.module.craftsman.vo.CraftsmanAuditDetailVO;
import com.lmf.heritage.module.craftsman.vo.CraftsmanExcelVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.List;

/**
 * 管理端 · 匠人管理（入驻资质审核）
 *
 * <p>审核动作打了 {@code @OperationLog}：资质结论直接决定一个账号能不能开展经营，
 * 「谁批的、什么时候批的、驳回理由写的什么」必须留痕。</p>
 *
 * @author 李明峰 23100025
 */
@Slf4j
@Tag(name = "管理端-匠人管理", description = "匠人列表、资质审核详情、通过/驳回、门类选项、导出")
@RestController
@RequestMapping("/admin/craftsman")
@RequiredArgsConstructor
@LoginRequired(roles = {Constants.ROLE_ADMIN})
public class AdminCraftsmanController {

    private final AdminCraftsmanService adminCraftsmanService;

    @Operation(summary = "匠人列表",
            description = "auditStatus 0待审核/1已通过/2已驳回，craftType 技艺门类，status 账号状态，"
                    + "keyword 匹配真实姓名/手机号/用户名/门类，startTime~endTime 申请时间范围；"
                    + "sort 待审优先(默认)/最新/关注数/作品数")
    @GetMapping("/list")
    public Result<PageResult<AdminCraftsmanVO>> list(AdminCraftsmanQueryDTO query) {
        return Result.success(adminCraftsmanService.page(query));
    }

    @Operation(summary = "资质审核详情",
            description = "全平台唯一返回证件号与资质图的接口，另附该匠人最近 5 件作品与 5 门课程的预览，"
                    + "便于判断申请人是否真在从事该技艺")
    @GetMapping("/{id}/audit-detail")
    public Result<CraftsmanAuditDetailVO> auditDetail(@PathVariable("id") Long id) {
        return Result.success(adminCraftsmanService.auditDetail(id));
    }

    @Operation(summary = "资质审核（通过 / 驳回）",
            description = "仅「待审核」状态可操作，重复审核返回 8002。pass=false 时 reason 必填（8006）。"
                    + "两种结果都会给申请人发站内消息；通过时会清空历史驳回原因")
    @OperationLog("匠人资质审核")
    @PostMapping("/audit")
    public Result<Void> audit(@RequestBody @Valid CraftsmanAuditDTO dto) {
        adminCraftsmanService.audit(dto);
        return Result.success();
    }

    @Operation(summary = "技艺门类选项", description = "取自现有匠人数据，供筛选下拉使用")
    @GetMapping("/craft-types")
    public Result<List<String>> craftTypes() {
        return Result.success(adminCraftsmanService.craftTypes());
    }

    @Operation(summary = "导出匠人 Excel",
            description = "筛选条件与列表一致，最多 5000 条。出于隐私考虑不导出证件号与资质图")
    @GetMapping("/export")
    public void export(AdminCraftsmanQueryDTO query, HttpServletResponse response) throws IOException {
        List<CraftsmanExcelVO> rows = adminCraftsmanService.exportList(query);

        response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        String fileName = URLEncoder.encode("匠人列表", StandardCharsets.UTF_8).replace("+", "%20");
        response.setHeader("Content-Disposition", "attachment;filename*=utf-8''" + fileName + ".xlsx");

        EasyExcel.write(response.getOutputStream(), CraftsmanExcelVO.class)
                .sheet("匠人列表")
                .doWrite(rows);
        log.info("[匠人管理] 导出 {} 条", rows.size());
    }
}
