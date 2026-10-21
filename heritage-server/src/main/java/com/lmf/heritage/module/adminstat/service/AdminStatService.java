package com.lmf.heritage.module.adminstat.service;

import com.lmf.heritage.module.adminstat.vo.PlatformStatVO;

/**
 * 管理端 · 平台数据可视化（规划书第六节 11 类图表）
 *
 * <p>11 张图分成三类处理，各自的补零规则不同：</p>
 * <ul>
 *   <li><b>分类 / 状态维度</b>（①⑥）：维度是封闭枚举或全量分类，
 *       缺哪个就等于 0，全部补齐 —— 饼图少一块、柱状图少一根，
 *       看图的人会以为「没有这个分类」，而不是「这个分类是 0」。</li>
 *   <li><b>时间维度</b>（⑤⑨⑪）：SQL 只返回有数据的区间，
 *       服务层按 LocalDate / YearMonth 循环补零。折线图缺格会把两点直接连起来，
 *       于是「1 号 10 单、3 号 10 单」被画成一条斜线，看起来像 2 号也有数据。</li>
 *   <li><b>排行类</b>（②⑦⑧）：不补零，Top N 有多少返回多少。</li>
 * </ul>
 *
 * @author 李明峰 23100025
 */
public interface AdminStatService {

    /** ① 非遗作品数量饼图（按分类，只算已通过审核的） */
    PlatformStatVO.GoodsCountPie goodsCountPie();

    /** ② 课程热度柱状图（按占用名额） */
    PlatformStatVO.CourseHeatBar courseHeat(Integer topN);

    /** ③ 用户访问与收藏量统计 */
    PlatformStatVO.VisitFavorite visitFavorite();

    /** ④ 商品销量饼图（按分类，件） */
    PlatformStatVO.SalesPie salesQuantityPie();

    /** ⑤ 平台交易金额折线图（按日，days 默认 30） */
    PlatformStatVO.SalesTrend salesTrend(Integer days);

    /** ⑤ 平台交易金额折线图（按月，months 默认 12） */
    PlatformStatVO.SalesTrend salesTrendMonthly(Integer months);

    /** ⑥ 订单状态统计（六种状态固定返回） */
    PlatformStatVO.OrderStatus orderStatus();

    /** ⑦ 优惠券领取使用统计 */
    PlatformStatVO.CouponUse couponUse();

    /** ⑧ 匠人销售排行榜 */
    PlatformStatVO.CraftsmanRank craftsmanRank(Integer topN);

    /** ⑨ 用户增长趋势（含累计，days 默认 30） */
    PlatformStatVO.UserGrowth userGrowth(Integer days);

    /** ⑩ 商品分类销售占比（按销售额） */
    PlatformStatVO.CategorySalesShare categorySalesShare();

    /** ⑪ 月度活跃用户趋势（months 默认 12） */
    PlatformStatVO.MonthlyActive monthlyActive(Integer months);

    /** 数据总览大屏 */
    PlatformStatVO.Overview overview();
}
