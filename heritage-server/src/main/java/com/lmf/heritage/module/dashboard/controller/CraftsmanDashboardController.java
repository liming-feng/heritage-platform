package com.lmf.heritage.module.dashboard.controller;

import com.lmf.heritage.annotation.LoginRequired;
import com.lmf.heritage.common.Constants;
import com.lmf.heritage.common.Result;
import com.lmf.heritage.module.dashboard.service.DashboardService;
import com.lmf.heritage.module.dashboard.vo.BookingCourseRankVO;
import com.lmf.heritage.module.dashboard.vo.BookingStatVO;
import com.lmf.heritage.module.dashboard.vo.DashboardOverviewVO;
import com.lmf.heritage.module.dashboard.vo.GoodsSalesTopVO;
import com.lmf.heritage.module.dashboard.vo.SalesTrendVO;
import com.lmf.heritage.module.dashboard.vo.TodoRemindVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 匠人端 · 数据看板（ECharts 数据源）
 *
 * <p><b>为什么拆成 6 个接口而不是一个 {@code /all}</b>：
 * 看板上有 4 张图表 + 2 组数字，它们的刷新频率和失败影响面完全不同——
 * 待办提醒要求实时，趋势图可以缓存一会儿；某一张图查挂了不该让整页白屏。
 * 拆开之后，前端可以并行请求、各自 loading、各自兜底，
 * 而且每个接口的返回值就是一张图的数据，前端不需要从大对象里再挖。</p>
 *
 * <p><b>权限</b>：整个类挂 {@code @LoginRequired}，匠人与管理员可访问
 * （管理员用于后台代运营视角）。数据范围由服务层按当前登录匠人圈定，
 * <b>接口不接收任何 craftsmanId 参数</b>——这是本项目防横向越权的统一做法：
 * 不给「传别人 id」留任何入口。</p>
 *
 * <p>所有接口都是 只读 GET，因此不加 {@code @OperationLog}：
 * 操作日志记录的是「谁改了什么」，看板刷新一次写一条日志只会把日志表刷爆。</p>
 *
 * @author 李明峰 23100025
 */
@Tag(name = "匠人端-数据看板", description = "概览卡片 / 销售趋势 / 商品销量榜 / 课程热度 / 预约指标 / 待办提醒")
@RestController
@RequestMapping("/craftsman/dashboard")
@RequiredArgsConstructor
@LoginRequired(roles = {Constants.ROLE_CRAFTSMAN, Constants.ROLE_ADMIN})
public class CraftsmanDashboardController {

    private final DashboardService dashboardService;

    @Operation(summary = "概览卡片",
            description = "本月销售额、本月订单数、本月预约人数、商品总浏览量，另附上月销售额与环比增长率。"
                    + "销售额口径为「已支付订单的实付金额」（待发货/待收货/已完成），按支付时间归属月份")
    @GetMapping("/overview")
    public Result<DashboardOverviewVO> overview() {
        return Result.success(dashboardService.overview());
    }

    @Operation(summary = "近 N 天销售趋势",
            description = "默认 7 天，最长 90 天。逐日返回销售额与订单数，"
                    + "没有订单的日子也会出现在结果里（金额为 0），折线图的时间轴因此不会缺格")
    @GetMapping("/sales-trend")
    public Result<SalesTrendVO> salesTrend(@RequestParam(value = "days", required = false) Integer days) {
        return Result.success(dashboardService.salesTrend(days));
    }

    @Operation(summary = "商品销量 Top N",
            description = "默认取 5 名、统计近 30 天，按成交件数降序。"
                    + "销量由订单项现算（不是商品表的累计销量），已退款与已取消订单不计入")
    @GetMapping("/goods-top")
    public Result<List<GoodsSalesTopVO>> goodsTop(@RequestParam(value = "topN", required = false) Integer topN,
                                                 @RequestParam(value = "days", required = false) Integer days) {
        return Result.success(dashboardService.goodsTop(topN, days));
    }

    @Operation(summary = "预约课程热度排行",
            description = "默认取 10 门，按占用名额数降序（并列时看预约单数）。"
                    + "一门课一个人都没约也会出现在结果里，值为 0")
    @GetMapping("/booking-rank")
    public Result<List<BookingCourseRankVO>> bookingRank(@RequestParam(value = "topN", required = false) Integer topN) {
        return Result.success(dashboardService.bookingRank(topN));
    }

    @Operation(summary = "预约专属指标",
            description = "待审核数、已通过未核销数、核销率（只在已结束课程上计算）、"
                    + "以及每门课程的名额使用率。核销率 = 实到人次 ÷（实到人次 + 应到未到人次）；"
                    + "没有已结束课程时返回 null 表示无从计算，而不是 0%")
    @GetMapping("/booking-stat")
    public Result<BookingStatVO> bookingStat() {
        return Result.success(dashboardService.bookingStat());
    }

    @Operation(summary = "待办提醒",
            description = "待发货订单、待审核预约、待退款申请，另有库存预警商品与待回复评价。"
                    + "每一项与对应列表接口使用同一套过滤条件，因此「看板数字 = 点进去的条数」")
    @GetMapping("/todo")
    public Result<TodoRemindVO> todo() {
        return Result.success(dashboardService.todo());
    }
}
