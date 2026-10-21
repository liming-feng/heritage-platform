package com.lmf.heritage.module.coupon.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 领券中心的一条券
 *
 * @author 李明峰 23100025
 */
@Data
@Schema(description = "领券中心的优惠券")
public class CouponVO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @Schema(description = "优惠券 id")
    private Long id;

    @Schema(description = "名称")
    private String name;

    @Schema(description = "类型 1满减 2折扣")
    private Integer type;

    @Schema(description = "类型描述")
    private String typeDesc;

    @Schema(description = "适用范围 1全场通用 2仅商品 3仅课程")
    private Integer scope;

    @Schema(description = "适用范围描述")
    private String scopeDesc;

    @Schema(description = "使用门槛金额")
    private BigDecimal threshold;

    @Schema(description = "满减金额（type=1）")
    private BigDecimal amount;

    @Schema(description = "折扣率 0-1（type=2）")
    private BigDecimal discount;

    @Schema(description = "优惠规则文案，如「满 200 减 30 元」")
    private String ruleText;

    @Schema(description = "发放总量")
    private Integer totalCount;

    @Schema(description = "已领取数量")
    private Integer receivedCount;

    @Schema(description = "剩余可领数量")
    private Integer remainCount;

    @Schema(description = "每人限领张数")
    private Integer perLimit;

    @Schema(description = "当前用户已领取张数")
    private Integer receivedByMe;

    @Schema(description = "当前用户是否还能领取")
    private Boolean canReceive;

    @Schema(description = "不能领取的原因（可领取时为 null）")
    private String blockReason;

    @Schema(description = "有效期开始")
    private LocalDateTime startTime;

    @Schema(description = "有效期结束")
    private LocalDateTime endTime;

    @Schema(description = "使用说明")
    private String description;
}
