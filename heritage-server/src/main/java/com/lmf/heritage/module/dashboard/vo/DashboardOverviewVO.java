package com.lmf.heritage.module.dashboard.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.math.BigDecimal;

/**
 * 匠人端 · 看板概览卡片
 *
 * <p><b>四个数字的口径（写清楚是为了让验收时能逐条核算）</b></p>
 * <ol>
 *   <li><b>本月销售额</b>：本月已支付订单的实付金额之和。只统计
 *       {@code status IN (1待发货, 2待收货, 3已完成)}——待付款(0)的钱还没到账、
 *       已退款(4)与已取消(5)要把钱退回去，都不该算进销售额。
 *       归属月份看 {@code pay_time} 而不是 {@code create_time}：一笔 8 月下单、9 月付款的订单，
 *       货款落在 9 月。</li>
 *   <li><b>本月订单数</b>：与销售额完全相同的过滤条件，所以「销售额 ÷ 订单数」就是客单价，
 *       两个数字不会互相打架。</li>
 *   <li><b>本月预约人数</b>：本月提交且<b>仍占着名额</b>的预约 {@code quota} 之和。
 *       「占着名额」= 状态 0待审核 / 1已通过 / 4已核销 / 5已完成，
 *       已拒绝(2)与已取消(3)都已把名额归还，不算进去。
 *       这个口径与课程名额使用率、种子数据的自洽规则是同一个，全项目只有这一套定义。</li>
 *   <li><b>商品总浏览量</b>：名下全部商品（含下架、待审核）的 {@code view_count} 之和。
 *       浏览量是 Redis 定时回写的，这里读的是已落库的值。</li>
 * </ol>
 *
 * <p>另外带回上月销售额与环比增长率：只有绝对值没有对比的卡片，
 * 看了也不知道生意在变好还是变差。</p>
 *
 * @author 李明峰 23100025
 */
@Data
@Schema(description = "匠人端 · 看板概览卡片")
public class DashboardOverviewVO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @Schema(description = "统计月份，如 2026-09")
    private String monthText;

    @Schema(description = "本月销售额（元）")
    private BigDecimal salesAmount;

    @Schema(description = "上月销售额（元），用于环比对比")
    private BigDecimal lastMonthAmount;

    @Schema(description = "销售额环比增长率（%，1 位小数；上月为 0 时返回 null 而不是无穷大）")
    private Double salesGrowthRate;

    @Schema(description = "本月订单数（已支付口径）")
    private Long orderCount;

    @Schema(description = "本月预约人数（仍占用名额的预约名额数之和）")
    private Long bookingPeople;

    @Schema(description = "商品总浏览量（名下全部商品累计）")
    private Long goodsViewTotal;
}
