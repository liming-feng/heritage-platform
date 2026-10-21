package com.lmf.heritage.module.coupon.strategy;

import com.lmf.heritage.module.coupon.entity.Coupon;

import java.math.BigDecimal;

/**
 * <b>优惠券抵扣策略（策略模式的核心接口）</b>
 *
 * <p>为什么这里一定要用策略模式而不是 if-else：</p>
 *
 * <p>「满减」和「折扣」的<b>计算规则完全不同</b>——
 * 满减是「减一个固定金额」，折扣是「按比例打折」。如果写成
 * {@code if (type == 1) {...} else if (type == 2) {...}}，
 * 那么这段判断会同时出现在三个地方（领券中心算展示文案、结算页算抵扣金额、
 * 下单时算实付金额），以后加一种「第 N 件半价」的券型就要改三处，
 * 漏改一处就会出现「结算页显示能减 20、下单只减了 10」这种对不上账的事故。</p>
 *
 * <p>抽成策略之后，实现类只负责「<b>给我一张券和一个金额，算出抵扣多少</b>」这一件事，
 * 调用方永远只写 {@code strategy.calculateDiscount(coupon, amount)}，
 * 新增券型只是多一个 {@code @Component} 实现类，{@link CouponStrategyFactory} 自动收录，
 * 所有既有代码一行都不用改（开闭原则）。</p>
 *
 * <p><b>调用约定：</b>本接口只做纯计算，<b>不查库、不改状态</b>。
 * 门槛是否满足由 {@link #meetThreshold} 单独判断，归属与状态校验由 Service 层负责，
 * 这样每个策略都能被单独单元测试。</p>
 *
 * @author 李明峰 23100025
 */
public interface CouponStrategy {

    /**
     * 本策略负责的券类型，与 {@code coupon.type} 对应
     *
     * <p>工厂据此建立 {@code type → 策略} 的映射，因此这个返回值必须唯一。</p>
     */
    int supportType();

    /**
     * 计算抵扣金额
     *
     * <p>约定：</p>
     * <ul>
     *   <li>返回值必然 {@code >= 0}，且<b>不会超过订单金额</b>（不能让实付变成负数）；</li>
     *   <li>统一保留 2 位小数，舍入方式 HALF_UP（四舍五入），与金额字段 DECIMAL(10,2) 对齐；</li>
     *   <li>不判断门槛，门槛由 {@link #meetThreshold} 判定后再调用。</li>
     * </ul>
     *
     * @param coupon 券定义
     * @param amount 订单金额（不含运费等其他费用）
     * @return 抵扣金额
     */
    BigDecimal calculateDiscount(Coupon coupon, BigDecimal amount);

    /**
     * 是否达到使用门槛
     *
     * <p>默认实现：订单金额 ≥ 券的门槛金额。门槛为 null 视作 0（无门槛券）。</p>
     */
    default boolean meetThreshold(Coupon coupon, BigDecimal amount) {
        if (amount == null) {
            return false;
        }
        BigDecimal threshold = coupon == null || coupon.getThreshold() == null
                ? BigDecimal.ZERO : coupon.getThreshold();
        return amount.compareTo(threshold) >= 0;
    }

    /**
     * 生成券的展示文案，例如「满 200 减 30」「满 100 享 85 折」
     *
     * <p>放在策略里而不是写死在 VO 组装逻辑中，是因为文案本身就依赖券型：
     * 满减要显示「减多少」，折扣要显示「打几折」，两者没办法用一套模板拼出来。</p>
     */
    String describe(Coupon coupon);
}
