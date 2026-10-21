package com.lmf.heritage.module.coupon.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 我的优惠券
 *
 * @author 李明峰 23100025
 */
@Data
@Schema(description = "我的优惠券")
public class UserCouponVO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @Schema(description = "用户券 id（下单时传这个，不是 couponId）")
    private Long id;

    @Schema(description = "券种 id")
    private Long couponId;

    @Schema(description = "持有人 user.id（下单校验归属用）")
    private Long userId;

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

    @Schema(description = "优惠规则文案")
    private String ruleText;

    @Schema(description = "状态 0未使用 1已使用 2已过期")
    private Integer status;

    @Schema(description = "状态描述")
    private String statusDesc;

    @Schema(description = "使用的订单号（已使用时）")
    private String orderNo;

    @Schema(description = "使用时间")
    private LocalDateTime usedTime;

    @Schema(description = "过期时间")
    private LocalDateTime expireTime;

    @Schema(description = "领取时间")
    private LocalDateTime createTime;

    @Schema(description = "使用说明")
    private String description;
}
