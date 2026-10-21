package com.lmf.heritage.module.adminstat.controller;

import com.lmf.heritage.annotation.LoginRequired;
import com.lmf.heritage.common.Constants;
import com.lmf.heritage.common.Result;
import com.lmf.heritage.module.adminstat.service.AdminStatService;
import com.lmf.heritage.module.adminstat.vo.PlatformStatVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 管理端 · 平台数据可视化（规划书第六节 11 类图表）
 *
 * <p>每个图表一个独立接口，而不是一个大而全的 /dashboard 返回所有东西：
 * 大屏上 11 张图的加载速度、刷新频率、失败影响面都不一样 ——
 * 「用户增长」一天变一次，「待办提醒」要分钟级刷新。
 * 拆开之后前端可以各刷各的，某张图 SQL 慢也不会拖垮整屏。</p>
 *
 * @author 李明峰 23100025
 */
@Tag(name = "管理端-数据可视化", description = "11 类图表 + 数据总览大屏")
@RestController
@RequestMapping("/admin/stat")
@RequiredArgsConstructor
@LoginRequired(roles = {Constants.ROLE_ADMIN})
public class AdminStatController {

    private final AdminStatService adminStatService;

    @Operation(summary = "数据总览大屏",
            description = "用户数 / 匠人数 / 在售作品 / 可约课程 / 订单数 / 交易额 / 有效预约数，"
                    + "外加四类待处理事项的角标数字")
    @GetMapping("/overview")
    public Result<PlatformStatVO.Overview> overview() {
        return Result.success(adminStatService.overview());
    }

    @Operation(summary = "① 非遗作品数量饼图",
            description = "按分类统计已通过审核的作品数（待审核的不算平台作品）。"
                    + "没有作品的分类也会返回，值为 0")
    @GetMapping("/goods-count-pie")
    public Result<PlatformStatVO.GoodsCountPie> goodsCountPie() {
        return Result.success(adminStatService.goodsCountPie());
    }

    @Operation(summary = "② 课程热度柱状图",
            description = "按「占用名额之和」排序，与预约监管、匠人看板同一口径（不是预约单数）。"
                    + "topN 默认 10、上限 50")
    @GetMapping("/course-heat")
    public Result<PlatformStatVO.CourseHeatBar> courseHeat(
            @RequestParam(value = "topN", required = false) Integer topN) {
        return Result.success(adminStatService.courseHeat(topN));
    }

    @Operation(summary = "③ 用户访问与收藏量统计",
            description = "商品 / 课程 / 资讯三段浏览量与合计，收藏总数及按目标类型分布")
    @GetMapping("/visit-favorite")
    public Result<PlatformStatVO.VisitFavorite> visitFavorite() {
        return Result.success(adminStatService.visitFavorite());
    }

    @Operation(summary = "④ 商品销量饼图",
            description = "按分类统计商品销量（件）。与 ⑩ 的区别：④ 看件数，⑩ 看金额，"
                    + "两者排名常常不一致，是两个不同的运营结论")
    @GetMapping("/sales-quantity-pie")
    public Result<PlatformStatVO.SalesPie> salesQuantityPie() {
        return Result.success(adminStatService.salesQuantityPie());
    }

    @Operation(summary = "⑤ 平台交易金额折线图（按日）",
            description = "只统计已支付且未取消的订单（status 1~4），日期连续补零。"
                    + "days 默认 30、上限 90")
    @GetMapping("/sales-trend")
    public Result<PlatformStatVO.SalesTrend> salesTrend(
            @RequestParam(value = "days", required = false) Integer days) {
        return Result.success(adminStatService.salesTrend(days));
    }

    @Operation(summary = "⑤ 平台交易金额折线图（按月）",
            description = "同一口径按月聚合，月份连续补零。months 默认 12、上限 36")
    @GetMapping("/sales-trend-month")
    public Result<PlatformStatVO.SalesTrend> salesTrendMonthly(
            @RequestParam(value = "months", required = false) Integer months) {
        return Result.success(adminStatService.salesTrendMonthly(months));
    }

    @Operation(summary = "⑥ 订单状态统计",
            description = "待付款 / 待发货 / 待收货 / 已完成 / 已退款 / 已取消 六种固定返回，"
                    + "没有数据的状态补 0，柱状图不会少一根")
    @GetMapping("/order-status")
    public Result<PlatformStatVO.OrderStatus> orderStatus() {
        return Result.success(adminStatService.orderStatus());
    }

    @Operation(summary = "⑦ 优惠券领取使用统计",
            description = "各券种的领取数、使用数与核销率；核销率口径 = 已使用 / 已领取")
    @GetMapping("/coupon-use")
    public Result<PlatformStatVO.CouponUse> couponUse() {
        return Result.success(adminStatService.couponUse());
    }

    @Operation(summary = "⑧ 匠人销售排行榜",
            description = "按「订单项 → 商品 → 匠人」归因（不用 orders.craftsman_id，"
                    + "一单含多匠人商品时它记不全）。topN 默认 10、上限 50")
    @GetMapping("/craftsman-rank")
    public Result<PlatformStatVO.CraftsmanRank> craftsmanRank(
            @RequestParam(value = "topN", required = false) Integer topN) {
        return Result.success(adminStatService.craftsmanRank(topN));
    }

    @Operation(summary = "⑨ 用户增长趋势",
            description = "每日新增与累计用户数，日期连续补零；累计数的基数是区间之前已有的用户数。"
                    + "days 默认 30、上限 90")
    @GetMapping("/user-growth")
    public Result<PlatformStatVO.UserGrowth> userGrowth(
            @RequestParam(value = "days", required = false) Integer days) {
        return Result.success(adminStatService.userGrowth(days));
    }

    @Operation(summary = "⑩ 商品分类销售占比",
            description = "按分类统计销售额（元）与占比")
    @GetMapping("/category-sales-share")
    public Result<PlatformStatVO.CategorySalesShare> categorySalesShare() {
        return Result.success(adminStatService.categorySalesShare());
    }

    @Operation(summary = "⑪ 月度活跃用户趋势",
            description = "活跃 = 当月有过下单、预约或浏览记录（跨来源去重）。"
                    + "月份连续补零，months 默认 12、上限 36")
    @GetMapping("/monthly-active")
    public Result<PlatformStatVO.MonthlyActive> monthlyActive(
            @RequestParam(value = "months", required = false) Integer months) {
        return Result.success(adminStatService.monthlyActive(months));
    }
}
