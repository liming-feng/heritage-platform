package com.lmf.heritage.module.adminstat.mapper;

import com.lmf.heritage.module.adminstat.vo.PlatformStatVO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * 管理端 · 平台数据可视化 Mapper（规划书第六节 11 类图表）
 *
 * <p>这一组查询与「业务模块的列表查询」有一个根本区别：
 * <b>它们不服务于操作，只服务于看图</b>。因此每条 SQL 都可以放心地
 * 全表聚合 —— 不走分页、不带登录身份过滤，代价由管理端这一个调用方承担。</p>
 *
 * <p>所有「按天 / 按月」的查询都<b>只返回有数据的区间</b>，缺的由服务层补零：
 * SQL 里造日历要写递归 CTE，难读；而 Java 里按 LocalDate 循环三行就够，
 * 补零的规则（补到哪一天、补几个）也只有一处。</p>
 *
 * @author 李明峰 23100025
 */
@Mapper
public interface AdminStatMapper {

    /** ① 按分类统计已通过审核的作品数量 */
    List<PlatformStatVO.PieItem> selectGoodsCountByCategory();

    /** ② 课程热度：按占用名额排序，取前 topN */
    List<PlatformStatVO.CourseHeatItem> selectCourseHeat(@Param("topN") int topN);

    /** ③ 浏览量合计：商品 / 课程 / 资讯 三段 */
    Map<String, Object> selectViewTotal();

    /** ③ 收藏按目标类型分组 */
    List<Map<String, Object>> selectFavoriteByType();

    /** ④ 按分类统计商品销量（件） */
    List<PlatformStatVO.PieItem> selectSalesQuantityByCategory();

    /** ⑤ 交易金额趋势：unit = day 按日 / month 按月 */
    List<PlatformStatVO.TrendPoint> selectSalesTrend(@Param("from") LocalDateTime from,
                                                    @Param("unit") String unit);

    /** ⑥ 订单按状态分组 */
    List<Map<String, Object>> selectOrderStatus();

    /** ⑦ 各券种的领取 / 使用情况 */
    List<PlatformStatVO.CouponUseItem> selectCouponUse();

    /** ⑧ 匠人销售排行：按商品归属匠人归因，取前 topN */
    List<PlatformStatVO.CraftsmanSalesItem> selectCraftsmanRank(@Param("topN") int topN);

    /** ⑨ 每日新增用户 */
    List<PlatformStatVO.TrendPoint> selectUserGrowth(@Param("from") LocalDateTime from);

    /** ⑩ 按分类统计销售额（元） */
    List<PlatformStatVO.PieItem> selectSalesAmountByCategory();

    /** ⑪ 月度活跃用户（下单 / 预约 / 浏览 任一即活跃，跨来源去重） */
    List<PlatformStatVO.TrendPoint> selectMonthlyActive(@Param("from") LocalDateTime from);

    /* ==================== 总览大屏 ==================== */

    Map<String, Object> selectOverview();
}
