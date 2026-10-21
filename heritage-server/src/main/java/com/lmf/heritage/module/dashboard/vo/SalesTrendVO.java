package com.lmf.heritage.module.dashboard.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serial;
import java.io.Serializable;
import java.math.BigDecimal;
import java.util.List;

/**
 * 近 N 天销售趋势（折线图数据源）
 *
 * <p><b>为什么日期轴由 Java 补全、而不是直接用 GROUP BY 的结果</b>：
 * 数据库只会返回「有订单的那些天」。如果 9 月 14 日一单都没有，SQL 结果里就没有这一天，
 * 折线图会把 13 日的点直接连到 15 日，<b>视觉上凭空少了一天</b>。
 * 所以这里在 Java 侧先把从起始日到今天为止的每一天都铺出来（值为 0），
 * 再把 SQL 查到的真实数据填进去。前端拿到的 {@code points} 长度恒等于请求的天数。</p>
 *
 * <p>每个点除了金额还带订单数：折线和柱线叠在一张图上时，
 * 「销售额高但订单少」往往意味着那几天卖出了高单价的藏品，是值得看的信息。</p>
 *
 * @author 李明峰 23100025
 */
@Data
@Schema(description = "销售趋势（折线图）")
public class SalesTrendVO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @Schema(description = "统计天数")
    private Integer days;

    @Schema(description = "区间起始日期（含），yyyy-MM-dd")
    private String startDate;

    @Schema(description = "区间结束日期（含），yyyy-MM-dd")
    private String endDate;

    @Schema(description = "区间销售额合计（元）")
    private BigDecimal totalAmount;

    @Schema(description = "区间日均销售额（元）")
    private BigDecimal avgAmount;

    @Schema(description = "区间单日最高销售额（元），供图表纵轴留白参考")
    private BigDecimal maxAmount;

    @Schema(description = "逐日数据点，按日期升序，无订单的日子金额为 0")
    private List<DayPoint> points;

    /**
     * 单日数据点
     */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Schema(description = "销售趋势的单日数据点")
    public static class DayPoint implements Serializable {

        @Serial
        private static final long serialVersionUID = 1L;

        @Schema(description = "日期 yyyy-MM-dd")
        private String date;

        @Schema(description = "横轴短标签，如 9-14")
        private String dateText;

        @Schema(description = "星期中文，如 周日")
        private String weekday;

        @Schema(description = "当日销售额（元）")
        private BigDecimal amount;

        @Schema(description = "当日订单数")
        private Long orderCount;

        @Schema(description = "当日是否下单日（用于前端把无单日显示为空心点）")
        private Boolean hasOrder;
    }
}
