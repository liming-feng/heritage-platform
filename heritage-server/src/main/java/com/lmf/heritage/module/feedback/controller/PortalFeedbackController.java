package com.lmf.heritage.module.feedback.controller;

import com.lmf.heritage.annotation.LoginRequired;
import com.lmf.heritage.annotation.RateLimit;
import com.lmf.heritage.common.PageResult;
import com.lmf.heritage.common.Result;
import com.lmf.heritage.module.feedback.dto.FeedbackDTO;
import com.lmf.heritage.module.feedback.dto.FeedbackQueryDTO;
import com.lmf.heritage.module.feedback.service.FeedbackService;
import com.lmf.heritage.module.feedback.vo.FeedbackVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 用户端 · 留言反馈
 *
 * <p>提交<b>不需要登录</b>：用户往往正是因为登录不上、或对账号有问题才来反馈，
 * 这时强制登录等于把反馈入口关掉。登录用户提交时会自动记录 userId，
 * 处理完成后能收到站内消息；游客提交则只能留联系方式。</p>
 *
 * @author 李明峰 23100025
 */
@Tag(name = "用户端-留言反馈", description = "提交反馈（无需登录）/ 我的反馈记录")
@RestController
@RequestMapping("/portal/feedback")
@RequiredArgsConstructor
public class PortalFeedbackController {

    private final FeedbackService feedbackService;

    @Operation(summary = "提交留言反馈",
            description = "类型 1功能建议 2内容纠错 3投诉举报 4其他；未登录也可提交，登录后提交会记录 userId 并推送处理结果")
    @RateLimit(count = 5, window = 300, dimension = "ip", message = "提交过于频繁，请稍后再试")
    @PostMapping("/submit")
    public Result<Void> submit(@RequestBody @Valid FeedbackDTO dto) {
        feedbackService.submit(dto);
        return Result.success();
    }

    @Operation(summary = "我的反馈记录", description = "只返回当前登录用户提交的反馈，含状态与管理员回复")
    @LoginRequired
    @GetMapping("/mine")
    public Result<PageResult<FeedbackVO>> mine(FeedbackQueryDTO query) {
        return Result.success(feedbackService.mine(query));
    }
}
