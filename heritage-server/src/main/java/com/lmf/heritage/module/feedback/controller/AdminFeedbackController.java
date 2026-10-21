package com.lmf.heritage.module.feedback.controller;

import com.alibaba.excel.EasyExcel;
import com.lmf.heritage.annotation.LoginRequired;
import com.lmf.heritage.annotation.OperationLog;
import com.lmf.heritage.common.Constants;
import com.lmf.heritage.common.PageResult;
import com.lmf.heritage.common.Result;
import com.lmf.heritage.module.feedback.dto.FeedbackBatchDTO;
import com.lmf.heritage.module.feedback.dto.FeedbackHandleDTO;
import com.lmf.heritage.module.feedback.dto.FeedbackQueryDTO;
import com.lmf.heritage.module.feedback.service.AdminFeedbackService;
import com.lmf.heritage.module.feedback.service.FeedbackService;
import com.lmf.heritage.module.feedback.vo.FeedbackExcelVO;
import com.lmf.heritage.module.feedback.vo.FeedbackStatVO;
import com.lmf.heritage.module.feedback.vo.FeedbackVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.List;

/**
 * 管理端 · 留言反馈处理
 *
 * <p>处理与批量处理都打 {@code @OperationLog}：回复内容会直接发到用户站内信，
 * 「谁承诺了什么」需要可追溯。</p>
 *
 * @author 李明峰 23100025
 */
@Slf4j
@Tag(name = "管理端-留言反馈", description = "反馈列表 / 类型筛选 / 状态流转与回复 / 批量处理 / 处理时效统计 / 导出")
@RestController
@RequestMapping("/admin/feedback")
@RequiredArgsConstructor
@LoginRequired(roles = {Constants.ROLE_ADMIN})
public class AdminFeedbackController {

    private final FeedbackService feedbackService;
    private final AdminFeedbackService adminFeedbackService;

    @Operation(summary = "反馈列表",
            description = "可按状态 status（0待处理 1处理中 2已完成）与类型 type"
                    + "（1功能建议 2内容纠错 3投诉举报 4其他）筛选；默认「待处理」排在前面")
    @GetMapping("/list")
    public Result<PageResult<FeedbackVO>> list(FeedbackQueryDTO query) {
        return Result.success(feedbackService.adminPage(query));
    }

    @Operation(summary = "处理反馈",
            description = "状态流转 0待处理 → 1处理中 → 2已完成，可同时填写回复；"
                    + "流转为「已完成」时回复必填，处理完成后给提交人发站内消息")
    @OperationLog("反馈处理")
    @PostMapping("/handle")
    public Result<Void> handle(@RequestBody @Valid FeedbackHandleDTO dto) {
        feedbackService.handle(dto);
        return Result.success();
    }

    @Operation(summary = "批量处理反馈",
            description = "单次最多 100 条，<b>整批回滚</b>：任何一条失败（如流转到已完成却没填回复）"
                    + "都会让整批不生效，不会出现「一半已回复、一半没有」的状态")
    @OperationLog("反馈批量处理")
    @PostMapping("/batch-handle")
    public Result<Integer> batchHandle(@RequestBody @Valid FeedbackBatchDTO dto) {
        return Result.success(adminFeedbackService.batchHandle(dto));
    }

    @Operation(summary = "处理时效统计",
            description = "总量与各状态数量、完成率、平均/最短/最长处理时长（只统计已完成的）、"
                    + "24 小时完成率、按类型分组统计、近 N 天每日提交量与完成量（日期连续补零）。"
                    + "days 默认 14、上限 90")
    @GetMapping("/stat")
    public Result<FeedbackStatVO> stat(@RequestParam(value = "days", required = false) Integer days) {
        return Result.success(adminFeedbackService.stat(days));
    }

    @Operation(summary = "导出反馈 Excel",
            description = "筛选条件与列表一致，最多 5000 条。含处理时长列（小时）；"
                    + "刻意不导出联系方式——反馈正文里已有用户自己填的联系方式")
    @GetMapping("/export")
    public void export(FeedbackQueryDTO query, HttpServletResponse response) throws IOException {
        List<FeedbackExcelVO> rows = adminFeedbackService.exportList(query);

        response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        String fileName = URLEncoder.encode("留言反馈", StandardCharsets.UTF_8).replace("+", "%20");
        response.setHeader("Content-Disposition", "attachment;filename*=utf-8''" + fileName + ".xlsx");

        EasyExcel.write(response.getOutputStream(), FeedbackExcelVO.class)
                .sheet("留言反馈")
                .doWrite(rows);
        log.info("[反馈管理] 导出 {} 条", rows.size());
    }
}
