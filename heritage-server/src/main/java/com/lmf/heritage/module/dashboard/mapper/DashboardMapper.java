package com.lmf.heritage.module.dashboard.mapper;

import com.lmf.heritage.module.dashboard.vo.BookingCourseRankVO;
import com.lmf.heritage.module.dashboard.vo.CourseQuotaVO;
import com.lmf.heritage.module.dashboard.vo.GoodsSalesTopVO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * 数据看板 Mapper
 *
 * <p><b>为什么单独建一个 Mapper，而不是把这几条统计挂到 GoodsMapper / OrderMapper 上</b>：
 * 看板的每一个数字都是<b>跨表</b>的——销售额来自 {@code orders}，预约人数来自 {@code booking}，
 * 商品浏览量来自 {@code goods}，课程热度还要 {@code course} 参与。
 * 如果把它们拆散塞进各模块的 Mapper，会出现两个问题：
 * 一是「看板的口径」被切碎到 4 个文件里，改一处口径要翻四个地方；
 * 二是各模块的 Mapper 会混进与自身职责无关的 SQL。
 * 所以这里按<b>使用场景</b>而不是按<b>表</b>来归置：看板要什么，就在这里写什么。</p>
 *
 * <p><b>越权隔离在哪一层</b>：所有方法的第一个参数都是 {@code craftsmanId}，
 * 且它一定出现在 WHERE 里（{@code o.craftsman_id = #{craftsmanId}} / {@code b.craftsman_id} /
 * {@code g.craftsman_id} / {@code c.craftsman_id}）。匠人 id 由服务层从登录态解析，
 * 前端没有任何途径传入别人的 id——即使传了也会被这里的条件过滤成空结果。</p>
 *
 * <p>返回 {@code Map<String,Object>} 的几个方法：聚合结果的字段名是 SQL 里写死的别名，
 * 在 Java 侧再包一个 VO 只是把值搬家一次，不如直接读 Map 来得短。
 * 服务层负责把 null 兜成 0 并做除法，保证「数据库里没有记录」和「有记录但值为 0」
 * 对前端是同一种表现。</p>
 *
 * @author 李明峰 23100025
 */
@Mapper
public interface DashboardMapper {

    /**
     * 概览卡片的四项指标 + 上月销售额
     *
     * <p>一次往返取回全部数字：这几个聚合各自都是小范围索引扫描，
     * 拆成 5 个接口只会把「打开看板」变成 5 次网络往返，收益是负的。</p>
     *
     * @param start     本月起始（含）
     * @param end       下月起始（不含），用左闭右开避免 23:59:59.999 的边界遗漏
     * @param lastStart 上月起始（含）
     * @param lastEnd   本月起始（不含）
     */
    Map<String, Object> selectOverview(@Param("craftsmanId") Long craftsmanId,
                                       @Param("start") LocalDateTime start,
                                       @Param("end") LocalDateTime end,
                                       @Param("lastStart") LocalDateTime lastStart,
                                       @Param("lastEnd") LocalDateTime lastEnd);

    /**
     * 逐日销售汇总（只返回有订单的日期，缺口由服务层补零）
     *
     * @return [{day: '2026-09-14', amount: 900.00, cnt: 2}, ...]
     */
    List<Map<String, Object>> selectSalesTrend(@Param("craftsmanId") Long craftsmanId,
                                               @Param("start") LocalDateTime start,
                                               @Param("end") LocalDateTime end);

    /** 商品销量 Top N（按统计期成交件数降序） */
    List<GoodsSalesTopVO> selectGoodsSalesTop(@Param("craftsmanId") Long craftsmanId,
                                              @Param("start") LocalDateTime start,
                                              @Param("end") LocalDateTime end,
                                              @Param("limit") int limit);

    /** 预约课程热度排行（按占用名额数降序） */
    List<BookingCourseRankVO> selectBookingCourseRank(@Param("craftsmanId") Long craftsmanId,
                                                      @Param("limit") int limit);

    /** 预约专属指标的原始聚合值 */
    Map<String, Object> selectBookingStat(@Param("craftsmanId") Long craftsmanId);

    /** 各课程名额使用率（按开课时间升序，让看板上的顺序与日历排期一致） */
    List<CourseQuotaVO> selectCourseQuotaUsage(@Param("craftsmanId") Long craftsmanId);

    /** 待办提醒的五个计数 */
    Map<String, Object> selectTodo(@Param("craftsmanId") Long craftsmanId,
                                   @Param("stockThreshold") int stockThreshold);
}
