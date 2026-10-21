package com.lmf.heritage.module.coupon.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.math.BigDecimal;
import java.util.List;

/**
 * 管理端 · 优惠券运营总览
 *
 * <p><b>核销率的口径</b>：已使用 / 已领取，不是 已使用 / 发放总量。
 * 用后者会把「还有一半没人领」算进分母，得出一个偏低的数字，
 * 让人误以为发出去的券没人用 —— 而真实情况可能是根本还没发完。</p>
 *
 * @author 李明峰 23100025
 */
@Data
@Schema(description = "优惠券运营总览")
public class CouponStatVO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @Schema(description = "券种数量")
    private Long couponTotal;

    @Schema(description = "启用中的券种数量")
    private Long activeCoupon;

    @Schema(description = "发放总量合计（所有券种的 total_count 之和）")
    private Long issuedTotal;

    @Schema(description = "累计已领取张数")
    private Long receivedTotal;

    @Schema(description = "未使用张数")
    private Long unusedTotal;

    @Schema(description = "已过期张数")
    private Long expiredTotal;

    @Schema(description = "已使用张数")
    private Long usedTotal;

    @Schema(description = "领取率 %（已领取 / 发放总量）")
    private BigDecimal receiveRate;

    @Schema(description = "核销率 %（已使用 / 已领取）")
    private BigDecimal useRate;

    @Schema(description = "按券类型分组的统计")
    private List<TypeStat> typeStats;

    @Schema(description = "核销率最高的券种 Top N（只统计已有领取记录的）")
    private List<CouponRank> useTop;

    @Data
    @Schema(description = "按类型分组的券统计")
    public static class TypeStat implements Serializable {

        @Serial
        private static final long serialVersionUID = 1L;

        @Schema(description = "类型 1满减 2折扣")
        private Integer type;

        @Schema(description = "类型描述")
        private String typeDesc;

        @Schema(description = "券种数量")
        private Long couponCount;

        @Schema(description = "已领取张数")
        private Long receivedCount;

        @Schema(description = "已使用张数")
        private Long usedCount;

        @Schema(description = "核销率 %")
        private BigDecimal useRate;
    }

    @Data
    @Schema(description = "券种核销排行项")
    public static class CouponRank implements Serializable {

        @Serial
        private static final long serialVersionUID = 1L;

        @Schema(description = "券 id")
        private Long couponId;

        @Schema(description = "券名称")
        private String couponName;

        @Schema(description = "类型描述")
        private String typeDesc;

        @Schema(description = "已领取张数")
        private Long receivedCount;

        @Schema(description = "已使用张数")
        private Long usedCount;

        @Schema(description = "核销率 %")
        private BigDecimal useRate;
    }
}
