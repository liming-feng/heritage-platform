package com.lmf.heritage.module.review.controller;

import com.lmf.heritage.annotation.LoginRequired;
import com.lmf.heritage.annotation.OperationLog;
import com.lmf.heritage.common.Constants;
import com.lmf.heritage.common.PageResult;
import com.lmf.heritage.common.Result;
import com.lmf.heritage.module.review.dto.ReviewQueryDTO;
import com.lmf.heritage.module.review.dto.ReviewReplyDTO;
import com.lmf.heritage.module.review.service.ReviewService;
import com.lmf.heritage.module.review.vo.CraftsmanReviewVO;
import com.lmf.heritage.module.review.vo.ReviewStatVO;
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
 * 匠人端 · 评价管理
 *
 * <p>商品评价与课程评价存在同一张 {@code review} 表里（靠 {@code type} 区分），
 * 所以这里是一个统一的工作台：不管评价来自商品还是课程，都按「我收到的评价」来查，
 * 不需要匠人先选是看商品还是课程。归属由服务层按 type 分流到
 * {@code goods.craftsman_id} 或 {@code course.craftsman_id}。</p>
 *
 * @author 李明峰 23100025
 */
@Tag(name = "匠人端-评价", description = "评价列表 / 回复 / 统计")
@RestController
@RequestMapping("/craftsman/review")
@RequiredArgsConstructor
@LoginRequired(roles = {Constants.ROLE_CRAFTSMAN, Constants.ROLE_ADMIN})
public class CraftsmanReviewController {

    private final ReviewService reviewService;

    @Operation(summary = "评价列表",
            description = "只看自己商品与课程收到的评价。支持 type（1商品/2课程）、targetId、"
                    + "rating（精确星级）、ratingLevel（good好评/mid中评/bad差评）、"
                    + "hasImage（只看带图）、replied（true已回复/false未回复）、keyword 与分页。"
                    + "未回复的排在前面，方便当待办清单处理")
    @GetMapping("/list")
    public Result<PageResult<CraftsmanReviewVO>> list(ReviewQueryDTO query) {
        return Result.success(reviewService.craftsmanReviews(query));
    }

    @Operation(summary = "回复 / 修改回复",
            description = "同一个接口既能首次回复也能修改已有回复，每次提交都会刷新回复时间。"
                    + "评价不属于自己的商品或课程时返回 1003")
    @OperationLog("回复评价")
    @PostMapping("/reply")
    public Result<Void> reply(@RequestBody @Valid ReviewReplyDTO dto) {
        reviewService.reply(dto);
        return Result.success();
    }

    @Operation(summary = "评价统计",
            description = "评价总数、平均分、好评/中评/差评数、好评率、带图数、已回复与未回复数、"
                    + "回复率，以及 5 星到 1 星的分布（可直接喂给图表）。"
                    + "口径与列表接口完全一致")
    @GetMapping("/stat")
    public Result<ReviewStatVO> stat() {
        return Result.success(reviewService.craftsmanStat());
    }
}
