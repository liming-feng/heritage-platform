package com.lmf.heritage.module.coupon.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.math.BigDecimal;

/**
 * 券核销结果
 *
 * <p>下单时除了抵扣金额，还需要把「用了哪张券」落到订单上：
 * {@code orders.coupon_id}（券种）与 {@code orders.user_coupon_id}（用户券）
 * 两个字段都要写。与其让订单模块自己再查一次券，不如这里一次返回，
 * 少一次查询，也不会出现「订单记的券 id 与真正核销的券不一致」。</p>
 *
 * @author 李明峰 23100025
 */
@Data
@Schema(description = "优惠券核销结果")
public class CouponUseResultVO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @Schema(description = "用户券 id（user_coupon.id）")
    private Long userCouponId;

    @Schema(description = "券种 id（coupon.id）")
    private Long couponId;

    @Schema(description = "券名称")
    private String name;

    @Schema(description = "优惠规则文案")
    private String ruleText;

    @Schema(description = "抵扣金额（保留 2 位小数）")
    private BigDecimal discountAmount;
}
