package com.lmf.heritage.module.coupon.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 结算页可选的一张券（含算好的抵扣额与抵扣后金额）
 *
 * @author 李明峰 23100025
 */
@Data
@Schema(description = "结算页可用优惠券")
public class CouponAvailableVO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @Schema(description = "用户券 id（下单时传这个）")
    private Long userCouponId;

    @Schema(description = "券种 id")
    private Long couponId;

    @Schema(description = "名称")
    private String name;

    @Schema(description = "类型 1满减 2折扣")
    private Integer type;

    @Schema(description = "类型描述")
    private String typeDesc;

    @Schema(description = "优惠规则文案")
    private String ruleText;

    @Schema(description = "使用门槛金额")
    private BigDecimal threshold;

    @Schema(description = "本单可抵扣金额")
    private BigDecimal discountAmount;

    @Schema(description = "使用后应付金额")
    private BigDecimal payAmount;

    @Schema(description = "是否可用（false 表示门槛没达到，仅作展示）")
    private Boolean usable;

    @Schema(description = "不可用原因（可用时为 null）")
    private String blockReason;

    @Schema(description = "过期时间")
    private LocalDateTime expireTime;
}
