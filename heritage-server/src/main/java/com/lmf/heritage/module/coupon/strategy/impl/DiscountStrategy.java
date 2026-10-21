package com.lmf.heritage.module.coupon.strategy.impl;

import com.lmf.heritage.common.Constants;
import com.lmf.heritage.module.coupon.entity.Coupon;
import com.lmf.heritage.module.coupon.strategy.CouponStrategy;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * <b>折扣策略</b>：满 {@code threshold} 享 {@code discount} 折
 *
 * <p>计算规则：抵扣额 = 订单金额 × (1 − 折扣率)</p>
 *
 * <p>字段语义：{@code coupon.discount} 存的是<b>折扣率</b>而不是「减掉的百分比」，
 * 也就是 85 折存 {@code 0.85}。这样满减和折扣两种券在「用户实际支付比例」上是同一套口径，
 * 结算页展示「原价 × 折扣率」时不需要再做二次换算。</p>
 *
 * <p>举例：100 元的订单用 0.85 的券 → 抵扣 100 × (1 − 0.85) = 15.00 → 实付 85.00。</p>
 *
 * <p>边界处理：</p>
 * <ul>
 *   <li>{@code discount} 为 null、≤ 0 或 ≥ 1 时视为无效券，抵扣 0 并记警告日志
 *       （≥ 1 表示不打折，≤ 0 表示白送，两者都属配置错误）；</li>
 *   <li>结果保留 2 位小数（HALF_UP），并再次与订单金额取 min，保证实付不为负。</li>
 * </ul>
 *
 * @author 李明峰 23100025
 */
@Slf4j
@Component
public class DiscountStrategy implements CouponStrategy {

    @Override
    public int supportType() {
        return Constants.COUPON_TYPE_DISCOUNT;
    }

    @Override
    public BigDecimal calculateDiscount(Coupon coupon, BigDecimal amount) {
        if (coupon == null || amount == null) {
            return zero();
        }
        BigDecimal rate = coupon.getDiscount();
        if (rate == null || rate.compareTo(BigDecimal.ZERO) <= 0
                || rate.compareTo(BigDecimal.ONE) >= 0) {
            log.warn("[优惠券] 折扣券 {} 的 discount 非法（应为 0~1 之间，实际 {}），按不抵扣处理",
                    coupon.getId(), rate);
            return zero();
        }

        // 抵扣 = 金额 × (1 - 折扣率)
        BigDecimal discount = amount.multiply(BigDecimal.ONE.subtract(rate))
                .setScale(2, RoundingMode.HALF_UP);

        // 双保险：理论上不会超过金额（rate > 0 时必然小于），但脏数据面前留一道闸
        if (discount.compareTo(amount) > 0) {
            return amount.setScale(2, RoundingMode.HALF_UP);
        }
        return discount;
    }

    @Override
    public String describe(Coupon coupon) {
        if (coupon == null || coupon.getDiscount() == null) {
            return "折扣券";
        }
        BigDecimal threshold = coupon.getThreshold() == null ? BigDecimal.ZERO : coupon.getThreshold();
        // 0.85 → 8.5 折；0.9 → 9 折
        String zhe = coupon.getDiscount().multiply(BigDecimal.TEN).stripTrailingZeros().toPlainString();
        if (threshold.compareTo(BigDecimal.ZERO) <= 0) {
            return "无门槛享 " + zhe + " 折";
        }
        return "满 " + threshold.stripTrailingZeros().toPlainString() + " 享 " + zhe + " 折";
    }

    private BigDecimal zero() {
        return BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
    }
}
