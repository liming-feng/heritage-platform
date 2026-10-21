package com.lmf.heritage.module.review.controller;

import com.lmf.heritage.annotation.LoginRequired;
import com.lmf.heritage.common.BusinessException;
import com.lmf.heritage.common.ErrorCode;
import com.lmf.heritage.common.PageResult;
import com.lmf.heritage.common.Result;
import com.lmf.heritage.module.review.dto.ReviewPublishCourseDTO;
import com.lmf.heritage.module.review.dto.ReviewPublishDTO;
import com.lmf.heritage.module.review.service.ReviewService;
import com.lmf.heritage.module.review.vo.ReviewVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 用户端 · 评价（读取在 P3，发表与点赞在 P4）
 *
 * @author 李明峰 23100025
 */
@Tag(name = "用户端-评价", description = "评价列表 / 发表评价 / 有用点赞")
@RestController
@RequestMapping("/portal/review")
@RequiredArgsConstructor
public class PortalReviewController {

    private final ReviewService reviewService;

    @Operation(summary = "评价列表", description = "type=1 商品评价 / type=2 课程评价")
    @GetMapping("/list")
    public Result<PageResult<ReviewVO>> list(
            @Parameter(description = "1商品评价 2课程评价") @RequestParam("type") Integer type,
            @Parameter(description = "商品 id 或课程 id") @RequestParam("targetId") Long targetId,
            @RequestParam(value = "pageNum", defaultValue = "1") Long pageNum,
            @RequestParam(value = "pageSize", defaultValue = "10") Long pageSize) {

        BusinessException.throwIf(type == null || targetId == null, ErrorCode.PARAM_ERROR, "type 与 targetId 不能为空");
        long size = Math.min(Math.max(pageSize == null ? 10 : pageSize, 1), 50);
        long num = Math.max(pageNum == null ? 1 : pageNum, 1);

        return Result.success(reviewService.pageByTarget(type, targetId, num, size));
    }

    @Operation(summary = "发表商品评价",
            description = "传入已完成订单里的 orderItemId，最多 3 张图；"
                    + "后端校验订单归属、订单状态与是否重复评价")
    @LoginRequired
    @PostMapping("/publish")
    public Result<Void> publish(@RequestBody @Valid ReviewPublishDTO dto) {
        reviewService.publish(dto);
        return Result.success();
    }

    @Operation(summary = "发表课程评价（P5）",
            description = "传入已完成预约的 bookingId，最多 3 张图；"
                    + "后端校验预约归属、预约状态（必须已完成）与是否重复评价。"
                    + "这是「提交 → 审核通过 → 核销 → 课程结束 → 已完成 → 评价」这条完整链路的最后一环")
    @LoginRequired
    @PostMapping("/publish-course")
    public Result<Void> publishCourse(@RequestBody @Valid ReviewPublishCourseDTO dto) {
        reviewService.publishCourse(dto);
        return Result.success();
    }

    @Operation(summary = "有用点赞", description = "同一用户对同一条评价只能点一次")
    @LoginRequired
    @PostMapping("/like/{id}")
    public Result<Void> like(@PathVariable("id") Long id) {
        reviewService.like(id);
        return Result.success();
    }
}
