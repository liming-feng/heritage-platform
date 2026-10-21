package com.lmf.heritage.module.content.controller;

import com.lmf.heritage.annotation.LoginRequired;
import com.lmf.heritage.annotation.RateLimit;
import com.lmf.heritage.common.PageResult;
import com.lmf.heritage.common.Result;
import com.lmf.heritage.module.content.dto.ArticleCommentDTO;
import com.lmf.heritage.module.content.service.ArticleCommentService;
import com.lmf.heritage.module.content.vo.ArticleCommentVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 用户端 · 资讯评论
 *
 * <p>查看无需登录，发表需要登录并限流 —— 评论是最容易被脚本刷的入口。</p>
 *
 * @author 李明峰 23100025
 */
@Tag(name = "用户端-资讯评论", description = "评论列表（主楼+回复）/ 发表评论 / 回复主楼")
@RestController
@RequestMapping("/portal/article/comment")
@RequiredArgsConstructor
public class PortalArticleCommentController {

    private final ArticleCommentService articleCommentService;

    @Operation(summary = "评论列表",
            description = "主楼分页，每条主楼带自己的 replies；只查一级评论（parent_id=0）作为分页单位")
    @GetMapping("/list")
    public Result<PageResult<ArticleCommentVO>> list(
            @RequestParam("articleId") Long articleId,
            @RequestParam(value = "pageNum", defaultValue = "1") long pageNum,
            @RequestParam(value = "pageSize", defaultValue = "10") long pageSize) {
        return Result.success(articleCommentService.page(articleId, pageNum, pageSize));
    }

    @Operation(summary = "发表评论 / 回复",
            description = "parentId 不传或为 0 表示发表主楼评论；非 0 表示回复该主楼（只支持一级回复）")
    @LoginRequired
    @RateLimit(count = 10, window = 60, dimension = "user", message = "评论过于频繁，请稍后再试")
    @PostMapping("/publish")
    public Result<Void> publish(@RequestBody @Valid ArticleCommentDTO dto) {
        articleCommentService.publish(dto);
        return Result.success();
    }
}
