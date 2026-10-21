package com.lmf.heritage.module.booking.controller;

import com.lmf.heritage.annotation.LoginRequired;
import com.lmf.heritage.annotation.RateLimit;
import com.lmf.heritage.common.PageResult;
import com.lmf.heritage.common.Result;
import com.lmf.heritage.module.booking.dto.BookingCancelDTO;
import com.lmf.heritage.module.booking.dto.BookingQueryDTO;
import com.lmf.heritage.module.booking.dto.BookingSubmitDTO;
import com.lmf.heritage.module.booking.service.BookingService;
import com.lmf.heritage.module.booking.vo.BookingVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * 用户端 · 预约（<b>课题题眼</b>）
 *
 * @author 李明峰 23100025
 */
@Tag(name = "用户端-预约", description = "提交预约 / 我的预约 / 取消 / 核销码")
@RestController
@RequestMapping("/portal/booking")
@RequiredArgsConstructor
public class PortalBookingController {

    private final BookingService bookingService;

    @Operation(summary = "提交预约",
            description = "三层防超卖：Redis 课程锁 → SELECT ... FOR UPDATE 校验 remain_quota → "
                    + "唯一索引 uk_user_course 防重复。限流维度为「按用户」，"
                    + "同一用户 60 秒内最多提交 5 次（不影响不同用户之间的并发抢名额）")
    @LoginRequired
    @RateLimit(count = 5, window = 60, dimension = "user", message = "预约提交过于频繁，请稍后再试")
    @PostMapping("/submit")
    public Result<BookingVO> submit(@RequestBody @Valid BookingSubmitDTO dto) {
        return Result.success(bookingService.submitAndDetail(dto));
    }

    @Operation(summary = "我的预约", description = "支持状态筛选 status、课程筛选 courseId、关键词 keyword、分页")
    @LoginRequired
    @GetMapping("/mine")
    public Result<PageResult<BookingVO>> mine(BookingQueryDTO query) {
        return Result.success(bookingService.myBookings(query));
    }

    @Operation(summary = "我的各状态预约数", description = "返回 {状态码: 数量}，外加 all 总数，用于 tab 角标")
    @LoginRequired
    @GetMapping("/count")
    public Result<Map<String, Long>> count() {
        return Result.success(bookingService.myStatusCount());
    }

    @Operation(summary = "预约详情", description = "只能查看自己的预约")
    @LoginRequired
    @GetMapping("/{id}")
    public Result<BookingVO> detail(@PathVariable("id") Long id) {
        return Result.success(bookingService.detail(id));
    }

    @Operation(summary = "取核销码",
            description = "仅「已通过 / 已核销」状态可看；前端用这个码渲染二维码，线下出示给匠人扫码核销")
    @LoginRequired
    @GetMapping("/{id}/checkin-code")
    public Result<String> checkinCode(@PathVariable("id") Long id) {
        return Result.success(bookingService.checkinCode(id));
    }

    @Operation(summary = "取消预约",
            description = "仅「待审核 / 已通过」可取消；会校验课程的 cancel_deadline 与配置里的 cancel_before_hours，"
                    + "取消后立即归还名额并发送站内消息")
    @LoginRequired
    @PostMapping("/cancel")
    public Result<Void> cancel(@RequestBody @Valid BookingCancelDTO dto) {
        bookingService.cancel(dto);
        return Result.success();
    }
}
