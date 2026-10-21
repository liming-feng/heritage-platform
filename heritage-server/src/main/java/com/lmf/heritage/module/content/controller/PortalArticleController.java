package com.lmf.heritage.module.content.controller;

import com.lmf.heritage.annotation.LoginRequired;
import com.lmf.heritage.common.PageResult;
import com.lmf.heritage.common.Result;
import com.lmf.heritage.module.content.dto.ArticleQueryDTO;
import com.lmf.heritage.module.content.service.ArticleLikeService;
import com.lmf.heritage.module.content.service.ArticleService;
import com.lmf.heritage.module.content.vo.ArticleDetailVO;
import com.lmf.heritage.module.content.vo.ArticleListVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 用户端 · 非遗资讯
 *
 * <p>资讯是「非遗文化科普」的载体：浏览无需登录，点赞与收藏在登录后才有状态。</p>
 *
 * @author 李明峰 23100025
 */
@Tag(name = "用户端-资讯", description = "资讯列表 / 详情 / 相关推荐 / 热门榜单 / 点赞")
@RestController
@RequestMapping("/portal/article")
@RequiredArgsConstructor
public class PortalArticleController {

    private final ArticleService articleService;
    private final ArticleLikeService articleLikeService;

    @Operation(summary = "资讯列表",
            description = "支持分类 categoryId、关键词 keyword、标签 tag、排序 sort（latest 最新 / hot 最热）、分页；"
                    + "只返回已发布内容，且不含正文")
    @GetMapping("/list")
    public Result<PageResult<ArticleListVO>> list(ArticleQueryDTO query) {
        return Result.success(articleService.pageList(query));
    }

    @Operation(summary = "热门资讯", description = "按浏览量排序的榜单，走 Redis 缓存")
    @GetMapping("/hot")
    public Result<List<ArticleListVO>> hot(@RequestParam(value = "limit", defaultValue = "5") int limit) {
        return Result.success(articleService.hot(limit));
    }

    @Operation(summary = "资讯详情",
            description = "返回正文、分类名、相关推荐（同分类优先，不足用热门补齐）、"
                    + "当前用户的点赞与收藏状态；同时把 Redis 浏览量计数器 +1 并叠加到 viewCount 上")
    @GetMapping("/{id}")
    public Result<ArticleDetailVO> detail(@PathVariable("id") Long id) {
        return Result.success(articleService.detail(id));
    }

    @Operation(summary = "点赞资讯",
            description = "一人一篇只能点一次：先查重，再由唯一索引 uk_user_article 兜底，成功后 like_count +1")
    @LoginRequired
    @PostMapping("/{id}/like")
    public Result<Void> like(@PathVariable("id") Long id) {
        articleLikeService.like(id);
        return Result.success();
    }

    @Operation(summary = "取消点赞", description = "幂等：没点过赞时也不会把计数减成负数")
    @LoginRequired
    @PostMapping("/{id}/unlike")
    public Result<Void> unlike(@PathVariable("id") Long id) {
        articleLikeService.unlike(id);
        return Result.success();
    }
}
