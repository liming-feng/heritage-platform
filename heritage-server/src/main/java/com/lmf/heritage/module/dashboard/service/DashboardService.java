package com.lmf.heritage.module.dashboard.service;

import com.lmf.heritage.module.dashboard.vo.BookingCourseRankVO;
import com.lmf.heritage.module.dashboard.vo.BookingStatVO;
import com.lmf.heritage.module.dashboard.vo.DashboardOverviewVO;
import com.lmf.heritage.module.dashboard.vo.GoodsSalesTopVO;
import com.lmf.heritage.module.dashboard.vo.SalesTrendVO;
import com.lmf.heritage.module.dashboard.vo.TodoRemindVO;

import java.util.List;

/**
 * 匠人端 · 数据看板服务
 *
 * <p>六个方法对应看板上的六块内容，全部<b>只读</b>，且全部<b>只见自己</b>。
 * 匠人 id 一律由 {@code CraftsmanHelper} 从登录态解析，方法签名里刻意不出现
 * {@code craftsmanId} 参数——接口里一旦有了这个参数，就总有人会想着从前端传一个进来。</p>
 *
 * <p>方法拆分粒度与前端页面一一对应：这样每个 ECharts 图表的加载失败互不影响，
 * 某个图表查挂了不会连累整页白屏。</p>
 *
 * @author 李明峰 23100025
 */
public interface DashboardService {

    /** 概览卡片：本月销售额 / 订单数 / 预约人数 / 商品总浏览量（含环比） */
    DashboardOverviewVO overview();

    /**
     * 近 N 天销售趋势（折线图）
     *
     * @param days 统计天数，null 时取 7；上限 90 天，避免一次拉爆图表
     */
    SalesTrendVO salesTrend(Integer days);

    /**
     * 商品销量 Top N（柱状图）
     *
     * @param topN 取前几名，null 时取 5
     * @param days 统计期天数，null 时取 30
     */
    List<GoodsSalesTopVO> goodsTop(Integer topN, Integer days);

    /**
     * 预约课程热度排行
     *
     * @param topN 取前几名，null 时取 10
     */
    List<BookingCourseRankVO> bookingRank(Integer topN);

    /** 预约专属指标：待审核 / 已通过未核销 / 核销率 / 各课程名额使用率 */
    BookingStatVO bookingStat();

    /** 待办提醒：待发货订单 / 待审核预约 / 待退款申请 / 库存预警 / 待回复评价 */
    TodoRemindVO todo();
}
