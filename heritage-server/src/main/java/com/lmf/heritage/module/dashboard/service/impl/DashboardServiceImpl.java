package com.lmf.heritage.module.dashboard.service.impl;

import com.lmf.heritage.common.Constants;
import com.lmf.heritage.module.course.enums.CourseStatusEnum;
import com.lmf.heritage.module.dashboard.mapper.DashboardMapper;
import com.lmf.heritage.module.dashboard.service.DashboardService;
import com.lmf.heritage.module.dashboard.vo.BookingCourseRankVO;
import com.lmf.heritage.module.dashboard.vo.BookingStatVO;
import com.lmf.heritage.module.dashboard.vo.CourseQuotaVO;
import com.lmf.heritage.module.dashboard.vo.DashboardOverviewVO;
import com.lmf.heritage.module.dashboard.vo.GoodsSalesTopVO;
import com.lmf.heritage.module.dashboard.vo.SalesTrendVO;
import com.lmf.heritage.module.dashboard.vo.TodoRemindVO;
import com.lmf.heritage.util.CraftsmanHelper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 匠人端 · 数据看板实现
 *
 * <p><b>这一层在做什么</b>：SQL 只负责「把原始的聚合值取回来」，
 * 所有需要判断的地方都在这里做——补零、算百分比、算环比、把 null 变成 0。
 * 原因是这些规则一旦写进 SQL，会被复制到 6 条语句里，改一处漏五处；
 * 放在 Java 里则是六个私有小方法，全世界的口径只有一份。</p>
 *
 * <p><b>为什么到处都在防 null</b>：聚合函数在「没有匹配行」时返回 null 而不是 0。
 * 一个新入驻、还没卖出任何东西的匠人打开看板，如果这里不兜底，
 * 页面上会出现「销售额 null」「核销率 NaN」。所以每一个取值都过一遍
 * {@link #num} / {@link #dec} 做归一。</p>
 *
 * @author 李明峰 23100025
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class DashboardServiceImpl implements DashboardService {

    private final DashboardMapper dashboardMapper;

    private final CraftsmanHelper craftsmanHelper;

    /** 趋势图默认天数 */
    private static final int DEFAULT_TREND_DAYS = 7;

    /** 趋势图最多天数：再多横轴就挤成一团，也没人看得清 */
    private static final int MAX_TREND_DAYS = 90;

    /** 商品销量榜默认取几名 */
    private static final int DEFAULT_GOODS_TOP = 5;

    /** 商品销量榜的统计窗口 */
    private static final int DEFAULT_GOODS_TOP_DAYS = 30;

    /** 课程热度默认取几名 */
    private static final int DEFAULT_RANK = 10;

    /** 榜单类接口的名次上限：一次要 500 名没有业务意义，只是给数据库添活 */
    private static final int MAX_TOP = 50;

    /** 商品销量榜统计窗口上限 */
    private static final int MAX_STAT_DAYS = 365;

    private static final DateTimeFormatter MONTH_FMT = DateTimeFormatter.ofPattern("yyyy-MM");

    /** 下标 0 对周一，与 {@link DayOfWeek#getValue()} 的 1~7 一一对应 */
    private static final String[] WEEK_CN = {"周一", "周二", "周三", "周四", "周五", "周六", "周日"};

    private static final BigDecimal HUNDRED = BigDecimal.valueOf(100);

    /* ==================================================================
     *  1. 概览卡片
     * ================================================================== */

    @Override
    public DashboardOverviewVO overview() {
        Long craftsmanId = craftsmanHelper.currentCraftsmanId();

        LocalDate today = LocalDate.now();
        LocalDate monthStart = today.withDayOfMonth(1);
        LocalDate nextMonthStart = monthStart.plusMonths(1);
        // 上月区间 = 上一个月的 1 日 到 本月 1 日（左闭右开）
        LocalDate lastMonthStart = monthStart.minusMonths(1);

        Map<String, Object> row = dashboardMapper.selectOverview(craftsmanId,
                monthStart.atStartOfDay(), nextMonthStart.atStartOfDay(),
                lastMonthStart.atStartOfDay(), monthStart.atStartOfDay());

        DashboardOverviewVO vo = new DashboardOverviewVO();
        // 月份文案取「本月 1 日」的格式，避免出现跨月那几天标错月份
        vo.setMonthText(MONTH_FMT.format(monthStart));

        BigDecimal salesAmount = dec(row.get("salesAmount"));
        BigDecimal lastMonthAmount = dec(row.get("lastMonthAmount"));
        vo.setSalesAmount(salesAmount);
        vo.setLastMonthAmount(lastMonthAmount);
        vo.setSalesGrowthRate(growthRate(salesAmount, lastMonthAmount));

        vo.setOrderCount(num(row.get("orderCount")));
        vo.setBookingPeople(num(row.get("bookingPeople")));
        vo.setGoodsViewTotal(num(row.get("goodsViewTotal")));

        log.debug("[看板] 匠人 {} 概览：本月销售额 {}，订单 {} 笔，预约 {} 人，浏览 {}",
                craftsmanId, salesAmount, vo.getOrderCount(), vo.getBookingPeople(), vo.getGoodsViewTotal());
        return vo;
    }

    /* ==================================================================
     *  2. 近 N 天销售趋势
     * ================================================================== */

    @Override
    public SalesTrendVO salesTrend(Integer days) {
        Long craftsmanId = craftsmanHelper.currentCraftsmanId();

        int span = clamp(days, DEFAULT_TREND_DAYS, MAX_TREND_DAYS);
        LocalDate endDate = LocalDate.now();
        LocalDate startDate = endDate.minusDays(span - 1L);

        // 右边界取「明天 0 点」，这样今天的订单能被完整覆盖到
        List<Map<String, Object>> rows = dashboardMapper.selectSalesTrend(craftsmanId,
                startDate.atStartOfDay(), endDate.plusDays(1).atStartOfDay());

        Map<String, BigDecimal> amountByDay = new HashMap<>(rows.size() * 2);
        Map<String, Long> countByDay = new HashMap<>(rows.size() * 2);
        for (Map<String, Object> row : rows) {
            String day = str(row.get("stat_day"));
            if (day == null || day.isEmpty()) {
                continue;
            }
            amountByDay.put(day, dec(row.get("amount")));
            countByDay.put(day, num(row.get("cnt")));
        }

        List<SalesTrendVO.DayPoint> points = new ArrayList<>(span);
        BigDecimal total = BigDecimal.ZERO;
        BigDecimal max = BigDecimal.ZERO;
        for (LocalDate cursor = startDate; !cursor.isAfter(endDate); cursor = cursor.plusDays(1)) {
            String key = cursor.toString();
            BigDecimal amount = amountByDay.getOrDefault(key, BigDecimal.ZERO);
            long cnt = countByDay.getOrDefault(key, 0L);

            points.add(new SalesTrendVO.DayPoint(
                    key,
                    cursor.getMonthValue() + "-" + cursor.getDayOfMonth(),
                    WEEK_CN[cursor.getDayOfWeek().getValue() - 1],
                    amount,
                    cnt,
                    cnt > 0));

            total = total.add(amount);
            if (amount.compareTo(max) > 0) {
                max = amount;
            }
        }

        SalesTrendVO vo = new SalesTrendVO();
        vo.setDays(span);
        vo.setStartDate(startDate.toString());
        vo.setEndDate(endDate.toString());
        vo.setTotalAmount(total);
        vo.setMaxAmount(max);
        vo.setAvgAmount(total.divide(BigDecimal.valueOf(span), 2, RoundingMode.HALF_UP));
        vo.setPoints(points);
        return vo;
    }

    /* ==================================================================
     *  3. 商品销量 Top N
     * ================================================================== */

    @Override
    public List<GoodsSalesTopVO> goodsTop(Integer topN, Integer days) {
        Long craftsmanId = craftsmanHelper.currentCraftsmanId();

        int limit = clamp(topN, DEFAULT_GOODS_TOP, MAX_TOP);
        int span = clamp(days, DEFAULT_GOODS_TOP_DAYS, MAX_STAT_DAYS);
        LocalDate endDate = LocalDate.now();
        LocalDate startDate = endDate.minusDays(span - 1L);

        List<GoodsSalesTopVO> list = dashboardMapper.selectGoodsSalesTop(craftsmanId,
                startDate.atStartOfDay(), endDate.plusDays(1).atStartOfDay(), limit);

        for (int i = 0; i < list.size(); i++) {
            GoodsSalesTopVO item = list.get(i);
            item.setRank(i + 1);
            item.setSalesQty(item.getSalesQty() == null ? 0L : item.getSalesQty());
            item.setSalesAmount(item.getSalesAmount() == null ? BigDecimal.ZERO : item.getSalesAmount());
        }
        return list;
    }

    /* ==================================================================
     *  4. 预约课程热度排行
     * ================================================================== */

    @Override
    public List<BookingCourseRankVO> bookingRank(Integer topN) {
        Long craftsmanId = craftsmanHelper.currentCraftsmanId();

        int limit = clamp(topN, DEFAULT_RANK, MAX_TOP);
        List<BookingCourseRankVO> list = dashboardMapper.selectBookingCourseRank(craftsmanId, limit);

        for (int i = 0; i < list.size(); i++) {
            BookingCourseRankVO item = list.get(i);
            item.setRank(i + 1);
            item.setStatusDesc(CourseStatusEnum.descOf(item.getStatus()));
            item.setBookingCount(item.getBookingCount() == null ? 0L : item.getBookingCount());
            long people = item.getBookingPeople() == null ? 0L : item.getBookingPeople();
            item.setBookingPeople(people);
            item.setUsedRate(percent(people, item.getTotalQuota() == null ? 0 : item.getTotalQuota()));
        }
        return list;
    }

    /* ==================================================================
     *  5. 预约专属指标
     * ================================================================== */

    @Override
    public BookingStatVO bookingStat() {
        Long craftsmanId = craftsmanHelper.currentCraftsmanId();

        Map<String, Object> row = dashboardMapper.selectBookingStat(craftsmanId);

        BookingStatVO vo = new BookingStatVO();
        vo.setWaitAudit(num(row.get("waitAudit")));
        vo.setPassedUnchecked(num(row.get("passedUnchecked")));
        vo.setCheckedCount(num(row.get("checkedCount")));
        vo.setValidCount(num(row.get("validCount")));
        vo.setEndedCourseCount(num(row.get("endedCourseCount")));

        long checkinPeople = num(row.get("checkinPeople"));
        long absentPeople = num(row.get("absentPeople"));
        vo.setCheckinPeople(checkinPeople);
        vo.setAbsentPeople(absentPeople);
        // 没有已结束课程时，分母为 0，指标是「无从计算」而不是「0%」——返回 null 让前端显示「—」
        vo.setCheckinRate(percent(checkinPeople, checkinPeople + absentPeople));

        List<CourseQuotaVO> courses = dashboardMapper.selectCourseQuotaUsage(craftsmanId);
        for (CourseQuotaVO course : courses) {
            course.setStatusDesc(CourseStatusEnum.descOf(course.getStatus()));
            course.setBookingCount(course.getBookingCount() == null ? 0L : course.getBookingCount());
            long used = course.getUsedQuota() == null ? 0L : course.getUsedQuota();
            course.setUsedQuota(used);
            course.setUsedRate(percent(used, course.getTotalQuota() == null ? 0 : course.getTotalQuota()));
        }
        vo.setCourses(courses);

        log.debug("[看板] 匠人 {} 预约指标：待审 {}，已通过未核销 {}，核销率 {}%（实到 {} / 应到未到 {}）",
                craftsmanId, vo.getWaitAudit(), vo.getPassedUnchecked(), vo.getCheckinRate(),
                checkinPeople, absentPeople);
        return vo;
    }

    /* ==================================================================
     *  6. 待办提醒
     * ================================================================== */

    @Override
    public TodoRemindVO todo() {
        Long craftsmanId = craftsmanHelper.currentCraftsmanId();

        // 库存预警阈值与商品模块共用同一个常量，防止两处各写一个 5
        Map<String, Object> row = dashboardMapper.selectTodo(craftsmanId, Constants.STOCK_WARN_THRESHOLD);

        TodoRemindVO vo = new TodoRemindVO();
        vo.setWaitDeliverOrders(num(row.get("waitDeliverOrders")));
        vo.setWaitAuditBookings(num(row.get("waitAuditBookings")));
        vo.setApplyingRefunds(num(row.get("applyingRefunds")));
        vo.setStockWarnGoods(num(row.get("stockWarnGoods")));
        vo.setUnrepliedReviews(num(row.get("unrepliedReviews")));

        vo.setTotal(vo.getWaitDeliverOrders() + vo.getWaitAuditBookings() + vo.getApplyingRefunds()
                + vo.getStockWarnGoods() + vo.getUnrepliedReviews());
        return vo;
    }

    /* ==================================================================
     *  私有工具
     * ================================================================== */

    /**
     * 把参数夹到合法区间
     *
     * <p>入参直接来自前端查询串，可能是 null 也可能是 99999。
     * 与其在每个调用点各写一次判断，不如统一在这里收口：
     * null 取默认值，越界取上下限。</p>
     */
    private int clamp(Integer value, int defaultValue, int max) {
        if (value == null || value <= 0) {
            return defaultValue;
        }
        return Math.min(value, max);
    }

    /**
     * 环比增长率（%）
     *
     * <p>上月为 0 时返回 null：分母是 0，增长「率」在数学上没有定义。
     * 前端遇到 null 显示「—」或「上月无销售」，比显示一个假的 100% 诚实。</p>
     */
    private Double growthRate(BigDecimal current, BigDecimal last) {
        if (last == null || last.compareTo(BigDecimal.ZERO) == 0) {
            return current != null && current.compareTo(BigDecimal.ZERO) > 0 ? null : 0.0D;
        }
        return current.subtract(last)
                .multiply(HUNDRED)
                .divide(last, 1, RoundingMode.HALF_UP)
                .doubleValue();
    }

    /**
     * 占比（0-100，1 位小数）
     *
     * <p>分母为 0 时返回 null，理由与环比一致。</p>
     */
    private Double percent(long part, long total) {
        if (total <= 0) {
            return null;
        }
        return BigDecimal.valueOf(part)
                .multiply(HUNDRED)
                .divide(BigDecimal.valueOf(total), 1, RoundingMode.HALF_UP)
                .doubleValue();
    }

    /** 聚合结果统一转 long：MySQL 的 COUNT 给 Long、SUM 给 BigDecimal，这里一并吃掉 */
    private long num(Object value) {
        if (value instanceof BigDecimal decimal) {
            return decimal.longValue();
        }
        if (value instanceof Number number) {
            return number.longValue();
        }
        return 0L;
    }

    /** 聚合结果统一转 BigDecimal：金额一律走 BigDecimal，不经过 double 以免出现 0.30000000000000004 */
    private BigDecimal dec(Object value) {
        if (value instanceof BigDecimal decimal) {
            return decimal.setScale(2, RoundingMode.HALF_UP);
        }
        if (value instanceof Number number) {
            return BigDecimal.valueOf(number.doubleValue()).setScale(2, RoundingMode.HALF_UP);
        }
        return BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
    }

    private String str(Object value) {
        return value == null ? null : value.toString();
    }
}
