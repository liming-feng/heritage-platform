package com.lmf.heritage.module.adminstat.service.impl;

import com.lmf.heritage.common.Constants;
import com.lmf.heritage.module.adminstat.mapper.AdminStatMapper;
import com.lmf.heritage.module.adminstat.service.AdminStatService;
import com.lmf.heritage.module.adminstat.vo.PlatformStatVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 管理端 · 平台数据可视化实现
 *
 * @author 李明峰 23100025
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AdminStatServiceImpl implements AdminStatService {

    /** 趋势默认天数 */
    private static final int DEFAULT_DAYS = 30;

    /** 趋势默认月数 */
    private static final int DEFAULT_MONTHS = 12;

    /** 排行默认条数 */
    private static final int DEFAULT_TOP_N = 10;

    private static final Map<Integer, String> ORDER_STATUS_DESC = Map.of(
            Constants.ORDER_WAIT_PAY, "待付款",
            Constants.ORDER_WAIT_DELIVER, "待发货",
            Constants.ORDER_WAIT_RECEIVE, "待收货",
            Constants.ORDER_FINISHED, "已完成",
            Constants.ORDER_REFUNDED, "已退款",
            Constants.ORDER_CANCELED, "已取消");

    private static final Map<Integer, String> FAVORITE_TYPE_DESC = Map.of(
            Constants.TARGET_GOODS, "商品",
            Constants.TARGET_COURSE, "课程",
            Constants.TARGET_ARTICLE, "资讯");

    private final AdminStatMapper adminStatMapper;

    /* ==================== ① 非遗作品数量饼图 ==================== */

    @Override
    public PlatformStatVO.GoodsCountPie goodsCountPie() {
        List<PlatformStatVO.PieItem> items = adminStatMapper.selectGoodsCountByCategory();
        long total = items.stream().mapToLong(x -> nz(x.getValue())).sum();
        items.forEach(x -> x.setPercent(percent(x.getValue(), total)));

        PlatformStatVO.GoodsCountPie vo = new PlatformStatVO.GoodsCountPie();
        vo.setTotal(total);
        vo.setItems(items);
        return vo;
    }

    /* ==================== ② 课程热度柱状图 ==================== */

    @Override
    public PlatformStatVO.CourseHeatBar courseHeat(Integer topN) {
        List<PlatformStatVO.CourseHeatItem> items =
                adminStatMapper.selectCourseHeat(topN == null || topN <= 0 ? DEFAULT_TOP_N : Math.min(topN, 50));
        long total = items.stream().mapToLong(x -> nz(x.getQuota())).sum();
        items.forEach(x -> x.setPercent(percent(x.getQuota(), total)));

        PlatformStatVO.CourseHeatBar vo = new PlatformStatVO.CourseHeatBar();
        vo.setCourseCount((long) items.size());
        vo.setItems(items);
        return vo;
    }

    /* ==================== ③ 用户访问与收藏量 ==================== */

    @Override
    public PlatformStatVO.VisitFavorite visitFavorite() {
        Map<String, Object> view = adminStatMapper.selectViewTotal();
        long goodsView = toLong(view.get("goodsView"));
        long courseView = toLong(view.get("courseView"));
        long articleView = toLong(view.get("articleView"));

        List<PlatformStatVO.PieItem> favItems = new ArrayList<>();
        long favTotal = 0L;
        for (Map<String, Object> row : adminStatMapper.selectFavoriteByType()) {
            int type = toInt(row.get("targetType"));
            long cnt = toLong(row.get("cnt"));
            favTotal += cnt;
            PlatformStatVO.PieItem item = new PlatformStatVO.PieItem();
            item.setName(FAVORITE_TYPE_DESC.getOrDefault(type, "其他"));
            item.setValue(cnt);
            favItems.add(item);
        }
        final long favTotalFinal = favTotal;
        favItems.forEach(x -> x.setPercent(percent(x.getValue(), favTotalFinal)));

        PlatformStatVO.VisitFavorite vo = new PlatformStatVO.VisitFavorite();
        vo.setGoodsView(goodsView);
        vo.setCourseView(courseView);
        vo.setArticleView(articleView);
        vo.setTotalView(goodsView + courseView + articleView);
        vo.setFavoriteTotal(favTotal);
        vo.setFavoriteItems(favItems);
        return vo;
    }

    /* ==================== ④ 商品销量饼图 ==================== */

    @Override
    public PlatformStatVO.SalesPie salesQuantityPie() {
        List<PlatformStatVO.PieItem> items = adminStatMapper.selectSalesQuantityByCategory();
        long total = items.stream().mapToLong(x -> nz(x.getValue())).sum();
        items.forEach(x -> x.setPercent(percent(x.getValue(), total)));

        PlatformStatVO.SalesPie vo = new PlatformStatVO.SalesPie();
        vo.setTotal(total);
        vo.setItems(items);
        return vo;
    }

    /* ==================== ⑤ 交易金额折线图 ==================== */

    @Override
    public PlatformStatVO.SalesTrend salesTrend(Integer days) {
        int n = (days == null || days <= 0) ? DEFAULT_DAYS : Math.min(days, 90);
        LocalDateTime from = LocalDate.now().minusDays(n - 1L).atStartOfDay();
        return buildTrend(adminStatMapper.selectSalesTrend(from, "day"), "day",
                dailyLabels(n), BigDecimal.ZERO);
    }

    @Override
    public PlatformStatVO.SalesTrend salesTrendMonthly(Integer months) {
        int n = (months == null || months <= 0) ? DEFAULT_MONTHS : Math.min(months, 36);
        YearMonth first = YearMonth.now().minusMonths(n - 1L);
        return buildTrend(adminStatMapper.selectSalesTrend(first.atDay(1).atStartOfDay(), "month"),
                "month", monthlyLabels(n), BigDecimal.ZERO);
    }

    private PlatformStatVO.SalesTrend buildTrend(List<PlatformStatVO.TrendPoint> rows, String unit,
                                                 List<String> labels, BigDecimal zero) {
        Map<String, PlatformStatVO.TrendPoint> byLabel = new HashMap<>();
        for (PlatformStatVO.TrendPoint p : rows) {
            byLabel.put(p.getLabel(), p);
        }

        List<PlatformStatVO.TrendPoint> points = new ArrayList<>(labels.size());
        BigDecimal totalAmount = zero;
        long orderCount = 0L;
        for (String label : labels) {
            PlatformStatVO.TrendPoint hit = byLabel.get(label);
            PlatformStatVO.TrendPoint p = new PlatformStatVO.TrendPoint();
            p.setLabel(label);
            p.setValue(hit == null || hit.getValue() == null ? BigDecimal.ZERO : hit.getValue());
            p.setCount(hit == null ? 0L : nz(hit.getCount()));
            points.add(p);
            totalAmount = totalAmount.add(p.getValue());
            orderCount += p.getCount();
        }

        PlatformStatVO.SalesTrend vo = new PlatformStatVO.SalesTrend();
        vo.setUnit(unit);
        vo.setTotalAmount(totalAmount);
        vo.setOrderCount(orderCount);
        vo.setPoints(points);
        return vo;
    }

    /* ==================== ⑥ 订单状态统计 ==================== */

    @Override
    public PlatformStatVO.OrderStatus orderStatus() {
        Map<Integer, Long> byStatus = new HashMap<>();
        for (Map<String, Object> row : adminStatMapper.selectOrderStatus()) {
            byStatus.put(toInt(row.get("status")), toLong(row.get("cnt")));
        }

        // 六种状态固定返回：状态位缺失与「数量是 0」在图上是两回事
        List<PlatformStatVO.PieItem> items = new ArrayList<>();
        long total = 0L;
        for (Integer status : ORDER_STATUS_DESC.keySet().stream().sorted().toList()) {
            long cnt = byStatus.getOrDefault(status, 0L);
            total += cnt;
            PlatformStatVO.PieItem item = new PlatformStatVO.PieItem();
            item.setName(ORDER_STATUS_DESC.get(status));
            item.setValue(cnt);
            items.add(item);
        }
        final long totalFinal = total;
        items.forEach(x -> x.setPercent(percent(x.getValue(), totalFinal)));

        PlatformStatVO.OrderStatus vo = new PlatformStatVO.OrderStatus();
        vo.setTotal(total);
        vo.setItems(items);
        return vo;
    }

    /* ==================== ⑦ 优惠券领取使用 ==================== */

    @Override
    public PlatformStatVO.CouponUse couponUse() {
        List<PlatformStatVO.CouponUseItem> items = adminStatMapper.selectCouponUse();
        long received = 0L;
        long used = 0L;
        for (PlatformStatVO.CouponUseItem x : items) {
            received += nz(x.getReceivedCount());
            used += nz(x.getUsedCount());
            x.setUseRate(percent(x.getUsedCount(), x.getReceivedCount()));
        }

        PlatformStatVO.CouponUse vo = new PlatformStatVO.CouponUse();
        vo.setReceivedTotal(received);
        vo.setUsedTotal(used);
        vo.setUseRate(percent(used, received));
        vo.setItems(items);
        return vo;
    }

    /* ==================== ⑧ 匠人销售排行 ==================== */

    @Override
    public PlatformStatVO.CraftsmanRank craftsmanRank(Integer topN) {
        List<PlatformStatVO.CraftsmanSalesItem> items =
                adminStatMapper.selectCraftsmanRank(topN == null || topN <= 0 ? DEFAULT_TOP_N : Math.min(topN, 50));
        BigDecimal total = items.stream()
                .map(x -> nzDec(x.getAmount()))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        items.forEach(x -> x.setPercent(percentDec(nzDec(x.getAmount()), total)));

        PlatformStatVO.CraftsmanRank vo = new PlatformStatVO.CraftsmanRank();
        vo.setTotalAmount(total);
        vo.setItems(items);
        return vo;
    }

    /* ==================== ⑨ 用户增长趋势 ==================== */

    @Override
    public PlatformStatVO.UserGrowth userGrowth(Integer days) {
        int n = (days == null || days <= 0) ? DEFAULT_DAYS : Math.min(days, 90);
        LocalDateTime from = LocalDate.now().minusDays(n - 1L).atStartOfDay();

        Map<String, Long> byDay = new HashMap<>();
        long newTotal = 0L;
        for (PlatformStatVO.TrendPoint p : adminStatMapper.selectUserGrowth(from)) {
            long cnt = nz(p.getCount());
            byDay.put(p.getLabel(), cnt);
            newTotal += cnt;
        }

        // 累计数的起点：区间之前已有的用户数。用它做基数往前累加，
        // 免得为了算累计再去查一次「区间之前有多少人」
        long total = toLong(selectUserTotal());
        long before = Math.max(total - newTotal, 0L);

        List<PlatformStatVO.TrendPoint> points = new ArrayList<>(n);
        long cumulative = before;
        LocalDate cursor = LocalDate.now().minusDays(n - 1L);
        for (int k = 0; k < n; k++) {
            String label = cursor.format(DateTimeFormatter.ISO_LOCAL_DATE);
            long added = byDay.getOrDefault(label, 0L);
            cumulative += added;
            PlatformStatVO.TrendPoint p = new PlatformStatVO.TrendPoint();
            p.setLabel(label);
            p.setValue(BigDecimal.valueOf(cumulative));
            p.setCount(added);
            points.add(p);
            cursor = cursor.plusDays(1);
        }

        PlatformStatVO.UserGrowth vo = new PlatformStatVO.UserGrowth();
        vo.setTotal(total);
        vo.setNewTotal(newTotal);
        vo.setPoints(points);
        return vo;
    }

    private Object selectUserTotal() {
        return adminStatMapper.selectOverview().get("userTotal");
    }

    /* ==================== ⑩ 商品分类销售占比 ==================== */

    @Override
    public PlatformStatVO.CategorySalesShare categorySalesShare() {
        List<PlatformStatVO.PieItem> items = adminStatMapper.selectSalesAmountByCategory();
        BigDecimal total = items.stream()
                .map(x -> BigDecimal.valueOf(nz(x.getValue())))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        items.forEach(x -> x.setPercent(percentDec(BigDecimal.valueOf(nz(x.getValue())), total)));

        PlatformStatVO.CategorySalesShare vo = new PlatformStatVO.CategorySalesShare();
        vo.setTotalAmount(total);
        vo.setItems(items);
        return vo;
    }

    /* ==================== ⑪ 月度活跃用户 ==================== */

    @Override
    public PlatformStatVO.MonthlyActive monthlyActive(Integer months) {
        int n = (months == null || months <= 0) ? DEFAULT_MONTHS : Math.min(months, 36);
        YearMonth first = YearMonth.now().minusMonths(n - 1L);

        Map<String, Long> byMonth = new HashMap<>();
        for (PlatformStatVO.TrendPoint p : adminStatMapper.selectMonthlyActive(first.atDay(1).atStartOfDay())) {
            byMonth.put(p.getLabel(), nz(p.getValue() == null ? 0L : p.getValue().longValue()));
        }

        List<PlatformStatVO.TrendPoint> points = new ArrayList<>(n);
        YearMonth cursor = first;
        for (int k = 0; k < n; k++) {
            String label = cursor.toString();
            PlatformStatVO.TrendPoint p = new PlatformStatVO.TrendPoint();
            p.setLabel(label);
            p.setValue(BigDecimal.valueOf(byMonth.getOrDefault(label, 0L)));
            p.setCount(byMonth.getOrDefault(label, 0L));
            points.add(p);
            cursor = cursor.plusMonths(1);
        }

        PlatformStatVO.MonthlyActive vo = new PlatformStatVO.MonthlyActive();
        vo.setMonths(n);
        vo.setPoints(points);
        return vo;
    }

    /* ==================== 总览大屏 ==================== */

    @Override
    public PlatformStatVO.Overview overview() {
        Map<String, Object> m = adminStatMapper.selectOverview();
        PlatformStatVO.Overview vo = new PlatformStatVO.Overview();
        vo.setUserTotal(toLong(m.get("userTotal")));
        vo.setCraftsmanTotal(toLong(m.get("craftsmanTotal")));
        vo.setGoodsOnSale(toLong(m.get("goodsOnSale")));
        vo.setCourseOpen(toLong(m.get("courseOpen")));
        vo.setOrderTotal(toLong(m.get("orderTotal")));
        vo.setPayAmount(toDecimal(m.get("payAmount")));
        vo.setBookingTotal(toLong(m.get("bookingTotal")));
        vo.setWaitGoods(toLong(m.get("waitGoods")));
        vo.setWaitCourse(toLong(m.get("waitCourse")));
        vo.setWaitCraftsman(toLong(m.get("waitCraftsman")));
        vo.setWaitFeedback(toLong(m.get("waitFeedback")));
        return vo;
    }

    /* ==================== 内部工具 ==================== */

    private static List<String> dailyLabels(int days) {
        List<String> labels = new ArrayList<>(days);
        LocalDate cursor = LocalDate.now().minusDays(days - 1L);
        for (int k = 0; k < days; k++) {
            labels.add(cursor.format(DateTimeFormatter.ISO_LOCAL_DATE));
            cursor = cursor.plusDays(1);
        }
        return labels;
    }

    private static List<String> monthlyLabels(int months) {
        List<String> labels = new ArrayList<>(months);
        YearMonth cursor = YearMonth.now().minusMonths(months - 1L);
        for (int k = 0; k < months; k++) {
            labels.add(cursor.toString());
            cursor = cursor.plusMonths(1);
        }
        return labels;
    }

    /** 百分比；分母为 0 时返回 null —— 0% 会被读成「有数据但为零」，而实际是「没有数据」 */
    private static BigDecimal percent(Long value, long total) {
        if (total <= 0 || value == null) {
            return null;
        }
        return BigDecimal.valueOf(value)
                .multiply(BigDecimal.valueOf(100))
                .divide(BigDecimal.valueOf(total), 2, RoundingMode.HALF_UP);
    }

    private static BigDecimal percentDec(BigDecimal value, BigDecimal total) {
        if (total == null || total.compareTo(BigDecimal.ZERO) <= 0 || value == null) {
            return null;
        }
        return value.multiply(BigDecimal.valueOf(100))
                .divide(total, 2, RoundingMode.HALF_UP);
    }

    private static long nz(Long v) {
        return v == null ? 0L : v;
    }

    private static BigDecimal nzDec(BigDecimal v) {
        return v == null ? BigDecimal.ZERO : v;
    }

    /**
     * COUNT / SUM(DECIMAL) 在不同版本驱动下可能返回 Long 或 BigDecimal，
     * 统一按 Number 取值 —— 否则 ClassCastException 只会在某台机器上复现。
     */
    private static long toLong(Object v) {
        if (v == null) {
            return 0L;
        }
        return v instanceof Number n ? n.longValue() : Long.parseLong(String.valueOf(v));
    }

    private static int toInt(Object v) {
        return (int) toLong(v);
    }

    private static BigDecimal toDecimal(Object v) {
        if (v == null) {
            return BigDecimal.ZERO;
        }
        return v instanceof BigDecimal b ? b : new BigDecimal(String.valueOf(v));
    }
}
