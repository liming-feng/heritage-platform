package com.lmf.heritage.module.home.controller;

import com.lmf.heritage.common.Result;
import com.lmf.heritage.module.content.vo.BannerVO;
import com.lmf.heritage.module.course.vo.CourseSimpleVO;
import com.lmf.heritage.module.craftsman.vo.CraftsmanSimpleVO;
import com.lmf.heritage.module.goods.vo.CategoryVO;
import com.lmf.heritage.module.goods.vo.GoodsListVO;
import com.lmf.heritage.module.home.service.HomeService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 用户端 · 首页聚合
 *
 * <p>按板块拆分而非一个大接口：前端可以并行请求、单板块独立缓存与失败降级。</p>
 *
 * @author 李明峰 23100025
 */
@Tag(name = "用户端-首页", description = "轮播 / 分类 / 热门商品 / 推荐匠人 / 体验活动预告")
@RestController
@RequestMapping("/portal/home")
@RequiredArgsConstructor
public class PortalHomeController {

    private final HomeService homeService;

    @Operation(summary = "首页轮播", description = "缓存 home:banner，30 分钟")
    @GetMapping("/banners")
    public Result<List<BannerVO>> banners() {
        return Result.success(homeService.banners());
    }

    @Operation(summary = "分类导航", description = "缓存 home:categories，1 小时")
    @GetMapping("/categories")
    public Result<List<CategoryVO>> categories() {
        return Result.success(homeService.categories());
    }

    @Operation(summary = "热门商品", description = "按销量排序取前 8，缓存 home:hotGoods，10 分钟")
    @GetMapping("/hotGoods")
    public Result<List<GoodsListVO>> hotGoods() {
        return Result.success(homeService.hotGoods());
    }

    @Operation(summary = "推荐匠人", description = "按关注数排序取前 4，缓存 home:craftsmen，30 分钟")
    @GetMapping("/craftsmen")
    public Result<List<CraftsmanSimpleVO>> craftsmen() {
        return Result.success(homeService.recommendCraftsmen());
    }

    @Operation(summary = "体验活动预告", description = "即将开课且未结束的课程，按开课时间升序，缓存 course:list，10 分钟")
    @GetMapping("/courses")
    public Result<List<CourseSimpleVO>> courses() {
        return Result.success(homeService.upcomingCourses());
    }
}
