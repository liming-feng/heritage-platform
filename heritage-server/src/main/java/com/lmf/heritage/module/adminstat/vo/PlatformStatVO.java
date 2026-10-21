package com.lmf.heritage.module.adminstat.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.math.BigDecimal;
import java.util.List;

/**
 * 管理端 · 平台数据可视化（规划书第六节 11 类图表）
 *
 * <p><b>为什么把 11 张图的返回结构都放在一个类里做内部类</b>：
 * 它们只在「管理端大屏」这一处使用，各自拆成 11 个顶层 VO 会让
 * {@code module.adminstat.vo} 包里出现一堆只用一次的类名（PieItem / BarItem /
 * TrendPoint …），读代码的人要先猜「这个 Item 是哪张图的」。嵌套在这里，
 * 名字本身就带上了归属。</p>
 *
 * <p><b>百分比一律在 Java 侧算</b>：SQL 里算比例需要把总量再查一遍或写窗口函数，
 * 而总量本来就要返回给前端做角标。在 Java 里算一次，两个数必然同源。</p>
 *
 * @author 李明峰 23100025
 */
@Data
@Schema(description = "平台数据可视化")
public class PlatformStatVO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /* ==================== 通用图元 ==================== */

    @Data
    @Schema(description = "饼图 / 柱状图的一项")
    public static class PieItem implements Serializable {

        @Serial
        private static final long serialVersionUID = 1L;

        @Schema(description = "名称（分类名 / 状态名 / 券名）")
        private String name;

        @Schema(description = "数值")
        private Long value;

        @Schema(description = "占比 %；总量为 0 时为 null，不谎报 0%")
        private BigDecimal percent;
    }

    @Data
    @Schema(description = "折线图的一点")
    public static class TrendPoint implements Serializable {

        @Serial
        private static final long serialVersionUID = 1L;

        @Schema(description = "横坐标（日期 yyyy-MM-dd 或月份 yyyy-MM）")
        private String label;

        @Schema(description = "主数值（金额 / 人数）")
        private BigDecimal value;

        @Schema(description = "附带的次要数值（订单数 / 新增数）")
        private Long count;
    }

    /* ==================== ① 非遗作品数量饼图 ==================== */

    @Data
    @Schema(description = "① 非遗作品数量分布（按分类）")
    public static class GoodsCountPie implements Serializable {

        @Serial
        private static final long serialVersionUID = 1L;

        @Schema(description = "作品总数（只统计已通过审核的）")
        private Long total;

        @Schema(description = "各分类数量")
        private List<PieItem> items;
    }

    /* ==================== ② 课程热度柱状图 ==================== */

    @Data
    @Schema(description = "② 课程热度榜（按占用名额）")
    public static class CourseHeatBar implements Serializable {

        @Serial
        private static final long serialVersionUID = 1L;

        @Schema(description = "参与统计的课程数")
        private Long courseCount;

        @Schema(description = "各课程热度")
        private List<CourseHeatItem> items;
    }

    @Data
    @Schema(description = "课程热度项")
    public static class CourseHeatItem implements Serializable {

        @Serial
        private static final long serialVersionUID = 1L;

        @Schema(description = "课程 id")
        private Long courseId;

        @Schema(description = "课程标题")
        private String title;

        @Schema(description = "占用名额之和（与预约监管、匠人看板同一口径）")
        private Long quota;

        @Schema(description = "预约单数")
        private Long bookingCount;

        @Schema(description = "报名人数占比 %")
        private BigDecimal percent;
    }

    /* ==================== ③ 用户访问与收藏量 ==================== */

    @Data
    @Schema(description = "③ 用户访问与收藏量统计")
    public static class VisitFavorite implements Serializable {

        @Serial
        private static final long serialVersionUID = 1L;

        @Schema(description = "商品浏览量")
        private Long goodsView;

        @Schema(description = "课程浏览量")
        private Long courseView;

        @Schema(description = "资讯浏览量")
        private Long articleView;

        @Schema(description = "总浏览量")
        private Long totalView;

        @Schema(description = "收藏总数")
        private Long favoriteTotal;

        @Schema(description = "收藏按目标类型分布：1商品 2课程 3资讯")
        private List<PieItem> favoriteItems;
    }

    /* ==================== ④ 商品销量饼图 ==================== */

    @Data
    @Schema(description = "④ 商品销量分布（按分类，单位：件）")
    public static class SalesPie implements Serializable {

        @Serial
        private static final long serialVersionUID = 1L;

        @Schema(description = "总销量")
        private Long total;

        @Schema(description = "各分类销量")
        private List<PieItem> items;
    }

    /* ==================== ⑤ 平台交易金额折线图 ==================== */

    @Data
    @Schema(description = "⑤ 平台交易金额趋势")
    public static class SalesTrend implements Serializable {

        @Serial
        private static final long serialVersionUID = 1L;

        @Schema(description = "统计粒度 day / month")
        private String unit;

        @Schema(description = "区间交易总额")
        private BigDecimal totalAmount;

        @Schema(description = "区间订单数")
        private Long orderCount;

        @Schema(description = "趋势点（日期连续补零）")
        private List<TrendPoint> points;
    }

    /* ==================== ⑥ 订单状态统计 ==================== */

    @Data
    @Schema(description = "⑥ 订单状态统计")
    public static class OrderStatus implements Serializable {

        @Serial
        private static final long serialVersionUID = 1L;

        @Schema(description = "订单总数")
        private Long total;

        @Schema(description = "各状态数量（六种状态固定返回，缺失补 0）")
        private List<PieItem> items;
    }

    /* ==================== ⑦ 优惠券领取使用统计 ==================== */

    @Data
    @Schema(description = "⑦ 优惠券领取使用统计")
    public static class CouponUse implements Serializable {

        @Serial
        private static final long serialVersionUID = 1L;

        @Schema(description = "累计领取")
        private Long receivedTotal;

        @Schema(description = "已使用")
        private Long usedTotal;

        @Schema(description = "整体核销率 %（已使用 / 已领取）")
        private BigDecimal useRate;

        @Schema(description = "各券种的领取 / 使用")
        private List<CouponUseItem> items;
    }

    @Data
    @Schema(description = "单个券种的领取使用情况")
    public static class CouponUseItem implements Serializable {

        @Serial
        private static final long serialVersionUID = 1L;

        @Schema(description = "券 id")
        private Long couponId;

        @Schema(description = "券名称")
        private String name;

        @Schema(description = "已领取")
        private Long receivedCount;

        @Schema(description = "已使用")
        private Long usedCount;

        @Schema(description = "核销率 %")
        private BigDecimal useRate;
    }

    /* ==================== ⑧ 匠人销售排行榜 ==================== */

    @Data
    @Schema(description = "⑧ 匠人销售排行榜")
    public static class CraftsmanRank implements Serializable {

        @Serial
        private static final long serialVersionUID = 1L;

        @Schema(description = "区间销售总额")
        private BigDecimal totalAmount;

        @Schema(description = "排行项")
        private List<CraftsmanSalesItem> items;
    }

    @Data
    @Schema(description = "匠人销售项")
    public static class CraftsmanSalesItem implements Serializable {

        @Serial
        private static final long serialVersionUID = 1L;

        @Schema(description = "匠人 id")
        private Long craftsmanId;

        @Schema(description = "匠人姓名")
        private String name;

        @Schema(description = "销售额")
        private BigDecimal amount;

        @Schema(description = "售出件数")
        private Long quantity;

        @Schema(description = "订单数")
        private Long orderCount;

        @Schema(description = "销售额占比 %")
        private BigDecimal percent;
    }

    /* ==================== ⑨ 用户增长趋势 ==================== */

    @Data
    @Schema(description = "⑨ 用户增长趋势")
    public static class UserGrowth implements Serializable {

        @Serial
        private static final long serialVersionUID = 1L;

        @Schema(description = "当前用户总数")
        private Long total;

        @Schema(description = "区间新增")
        private Long newTotal;

        @Schema(description = "每日新增与累计（日期连续补零）")
        private List<TrendPoint> points;
    }

    /* ==================== ⑩ 商品分类销售占比 ==================== */

    @Data
    @Schema(description = "⑩ 商品分类销售额占比")
    public static class CategorySalesShare implements Serializable {

        @Serial
        private static final long serialVersionUID = 1L;

        @Schema(description = "销售额合计")
        private BigDecimal totalAmount;

        @Schema(description = "各分类销售额与占比")
        private List<PieItem> items;
    }

    /* ==================== ⑪ 月度活跃用户趋势 ==================== */

    @Data
    @Schema(description = "⑪ 月度活跃用户趋势")
    public static class MonthlyActive implements Serializable {

        @Serial
        private static final long serialVersionUID = 1L;

        @Schema(description = "统计月数")
        private Integer months;

        @Schema(description = "各月活跃用户数（有过下单 / 预约 / 浏览的去重用户）")
        private List<TrendPoint> points;
    }

    /* ==================== 总览大屏 ==================== */

    @Data
    @Schema(description = "数据总览大屏")
    public static class Overview implements Serializable {

        @Serial
        private static final long serialVersionUID = 1L;

        @Schema(description = "用户总数")
        private Long userTotal;

        @Schema(description = "匠人总数（已通过审核）")
        private Long craftsmanTotal;

        @Schema(description = "在售作品数")
        private Long goodsOnSale;

        @Schema(description = "可预约课程数")
        private Long courseOpen;

        @Schema(description = "累计订单数")
        private Long orderTotal;

        @Schema(description = "累计交易额")
        private BigDecimal payAmount;

        @Schema(description = "累计预约数（有效）")
        private Long bookingTotal;

        @Schema(description = "待处理事项：待审商品 / 待审课程 / 待审匠人 / 待处理反馈")
        private Long waitGoods;
        private Long waitCourse;
        private Long waitCraftsman;
        private Long waitFeedback;
    }
}
