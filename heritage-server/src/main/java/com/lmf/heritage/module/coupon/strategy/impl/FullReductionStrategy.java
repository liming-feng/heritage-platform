package com.lmf.heritage.module.coupon.strategy.impl;

import com.lmf.heritage.common.Constants;
import com.lmf.heritage.module.coupon.entity.Coupon;
import com.lmf.heritage.module.coupon.strategy.CouponStrategy;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * <b>满减策略</b>：满 {@code threshold} 减 {@code amount}
 *
 * <p>计算规则：抵扣额 = min(券面金额, 订单金额)</p>
 *
 * <p>为什么要取 min：一张「满 100 减 30」的券，如果订单只有 20 元（比如后面
 * 商品被下架导致金额变化）也不该减成负数。取 min 保证实付 ≥ 0。</p>
 *
 * <p>边界：如果券的 {@code amount} 字段为 null（脏数据），按「不抵扣」处理并记日志，
 * 而不是抛异常把整个结算流程打断。</p>
 *
 * @author 李明峰 23100025
 */
@Slf4j
@Component
public class FullReductionStrategy implements CouponStrategy {

    @Override
    public int supportType() {
        return Constants.COUPON_TYPE_FULL_REDUCTION;
    }

    @Override
    public BigDecimal calculateDiscount(Coupon coupon, BigDecimal amount) {
        if (coupon == null || amount == null) {
            return zero();
        }
        if (coupon.getAmount() == null || coupon.getAmount().compareTo(BigDecimal.ZERO) <= 0) {
            log.warn("[优惠券] 满减券 {} 的 amount 非法（{}），按不抵扣处理",
                    coupon.getId(), coupon.getAmount());
            return zero();
        }
        // 抵扣额不得超过订单金额，否则实付会变成负数
        BigDecimal discount = coupon.getAmount().min(amount);
        return discount.setScale(2, RoundingMode.HALF_UP);
    }

    @Override
    public String describe(Coupon coupon) {
        if (coupon == null) {
            return "满减券";
        }
        BigDecimal threshold = coupon.getThreshold() == null ? BigDecimal.ZERO : coupon.getThreshold();
        BigDecimal amount = coupon.getAmount() == null ? BigDecimal.ZERO : coupon.getAmount();
        if (threshold.compareTo(BigDecimal.ZERO) <= 0) {
            return "无门槛减 " + plain(amount) + " 元";
        }
        return "满 " + plain(threshold) + " 减 " + plain(amount) + " 元";
    }

    /** 去掉多余的 0：200.00 → 200、30.50 → 30.5 */
    private String plain(BigDecimal v) {
        return v.stripTrailingZeros().toPlainString();
    }

    private BigDecimal zero() {
        return BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
    }
}
