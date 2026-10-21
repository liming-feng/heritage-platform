package com.lmf.heritage.module.coupon.strategy;

import com.lmf.heritage.common.BusinessException;
import com.lmf.heritage.module.coupon.entity.Coupon;
import com.lmf.heritage.module.coupon.strategy.impl.DiscountStrategy;
import com.lmf.heritage.module.coupon.strategy.impl.FullReductionStrategy;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 优惠券策略模式单元测试（纯计算，不查库不改状态）
 *
 * <p>覆盖：满减策略、折扣策略、策略工厂的类型路由与重复注册快速失败、
 * 门槛判定、展示文案、全部边界（脏数据 / 抵扣封顶 / null 入参）。</p>
 *
 * <p>字段语义约定（与库表一致）：满减券 amount=券面金额；折扣券 discount=折扣率
 * （85 折存 0.85）；threshold=使用门槛，null 视为无门槛。</p>
 */
class CouponStrategyTest {

    private FullReductionStrategy fullReduction;
    private DiscountStrategy discount;
    private CouponStrategyFactory factory;

    @BeforeEach
    void setUp() {
        fullReduction = new FullReductionStrategy();
        discount = new DiscountStrategy();
        factory = new CouponStrategyFactory(List.of(fullReduction, discount));
    }

    private Coupon coupon(int type, String threshold, String amount, String discountRate) {
        Coupon c = new Coupon();
        c.setId(1L);
        c.setName("测试券");
        c.setType(type);
        c.setThreshold(threshold == null ? null : new BigDecimal(threshold));
        c.setAmount(amount == null ? null : new BigDecimal(amount));
        c.setDiscount(discountRate == null ? null : new BigDecimal(discountRate));
        return c;
    }

    /* ============ 满减策略 ============ */

    @Test
    @DisplayName("满减：满 200 减 30，订单 500 抵扣 30")
    void fullReductionNormal() {
        BigDecimal d = fullReduction.calculateDiscount(
                coupon(1, "200", "30", null), new BigDecimal("500"));
        assertEquals(new BigDecimal("30.00"), d);
    }

    @Test
    @DisplayName("满减：订单恰好等于门槛时可用（>= 而非 >）")
    void fullReductionAtThreshold() {
        BigDecimal d = fullReduction.calculateDiscount(
                coupon(1, "200", "30", null), new BigDecimal("200"));
        assertEquals(new BigDecimal("30.00"), d);
    }

    @Test
    @DisplayName("满减：券面金额超过订单金额时封顶为订单金额，实付不为负")
    void fullReductionCapped() {
        BigDecimal d = fullReduction.calculateDiscount(
                coupon(1, "100", "30", null), new BigDecimal("20"));
        assertEquals(new BigDecimal("20.00"), d, "抵扣额 = min(券面, 订单金额)");
    }

    @Test
    @DisplayName("满减：amount 为 null 或 ≤0 的脏数据按不抵扣处理")
    void fullReductionDirtyAmount() {
        assertEquals(new BigDecimal("0.00"),
                fullReduction.calculateDiscount(coupon(1, "100", null, null), new BigDecimal("500")));
        assertEquals(new BigDecimal("0.00"),
                fullReduction.calculateDiscount(coupon(1, "100", "-5", null), new BigDecimal("500")));
    }

    /* ============ 折扣策略 ============ */

    @Test
    @DisplayName("折扣：85 折（0.85）订单 100 抵扣 15.00")
    void discountNormal() {
        BigDecimal d = discount.calculateDiscount(
                coupon(2, "100", null, "0.85"), new BigDecimal("100"));
        assertEquals(new BigDecimal("15.00"), d);
    }

    @Test
    @DisplayName("折扣：四舍五入到分（HALF_UP），100×(1-0.855)=14.50")
    void discountRounding() {
        BigDecimal d = discount.calculateDiscount(
                coupon(2, null, null, "0.855"), new BigDecimal("100"));
        assertEquals(new BigDecimal("14.50"), d);
    }

    @Test
    @DisplayName("折扣：rate 为 null/0/1/负数时视为无效券，抵扣 0")
    void discountDirtyRate() {
        assertEquals(new BigDecimal("0.00"),
                discount.calculateDiscount(coupon(2, null, null, null), new BigDecimal("100")));
        assertEquals(new BigDecimal("0.00"),
                discount.calculateDiscount(coupon(2, null, null, "0"), new BigDecimal("100")));
        assertEquals(new BigDecimal("0.00"),
                discount.calculateDiscount(coupon(2, null, null, "1"), new BigDecimal("100")));
        assertEquals(new BigDecimal("0.00"),
                discount.calculateDiscount(coupon(2, null, null, "-0.1"), new BigDecimal("100")));
    }

    @Test
    @DisplayName("两种策略对 null 入参都返回 0.00 而不是抛 NPE")
    void nullInputs() {
        assertEquals(new BigDecimal("0.00"), fullReduction.calculateDiscount(null, new BigDecimal("100")));
        assertEquals(new BigDecimal("0.00"), discount.calculateDiscount(null, new BigDecimal("100")));
        assertEquals(new BigDecimal("0.00"), fullReduction.calculateDiscount(coupon(1, "0", "30", null), null));
    }

    /* ============ 门槛判定 ============ */

    @Test
    @DisplayName("门槛：订单金额 ≥ threshold 才可用，threshold 为 null 视为 0")
    void meetThreshold() {
        Coupon c = coupon(1, "200", "30", null);
        assertTrue(fullReduction.meetThreshold(c, new BigDecimal("200")));
        assertFalse(fullReduction.meetThreshold(c, new BigDecimal("199.99")));

        Coupon noThreshold = coupon(1, null, "10", null);
        assertTrue(fullReduction.meetThreshold(noThreshold, new BigDecimal("0.01")));
        assertFalse(fullReduction.meetThreshold(noThreshold, null));
    }

    /* ============ 展示文案 ============ */

    @Test
    @DisplayName("文案：满 200 减 30 / 无门槛减 10 元 / 满 100 享 8.5 折")
    void describe() {
        assertEquals("满 200 减 30 元",
                fullReduction.describe(coupon(1, "200", "30", null)));
        assertEquals("无门槛减 10 元",
                fullReduction.describe(coupon(1, "0", "10", null)));
        assertEquals("满 100 享 8.5 折",
                discount.describe(coupon(2, "100", null, "0.85")));
        assertEquals("无门槛享 9 折",
                discount.describe(coupon(2, null, null, "0.9")));
    }

    /* ============ 工厂 ============ */

    @Test
    @DisplayName("工厂按 type 路由：type=1 走满减，type=2 走折扣")
    void factoryRouting() {
        assertSame(fullReduction, factory.of(1));
        assertSame(discount, factory.of(2));
        assertEquals(new BigDecimal("30.00"),
                factory.calculateDiscount(coupon(1, "200", "30", null), new BigDecimal("500")));
        assertEquals(new BigDecimal("15.00"),
                factory.calculateDiscount(coupon(2, "100", null, "0.85"), new BigDecimal("100")));
    }

    @Test
    @DisplayName("工厂：未注册的 type 抛业务异常；null 券抵扣 0；null 券文案降级不抛异常")
    void factoryEdge() {
        BusinessException ex = assertThrows(BusinessException.class, () -> factory.of(99));
        assertNotNull(ex.getMessage());

        assertEquals(new BigDecimal("0.00"), factory.calculateDiscount(null, new BigDecimal("100")));
        assertEquals("优惠券", factory.describe(null));
        assertEquals("优惠券", factory.describe(coupon(99, null, null, null)));
    }

    @Test
    @DisplayName("工厂：同一 type 被两个策略注册时启动即快速失败")
    void duplicateRegistrationFailsFast() {
        // 两个满减策略注册同一个 type=1
        IllegalStateException ex = assertThrows(IllegalStateException.class,
                () -> new CouponStrategyFactory(List.of(
                        new FullReductionStrategy(),
                        new FullReductionStrategy())));
        assertTrue(ex.getMessage().contains("重复注册"), "错误信息应说明是重复注册：" + ex.getMessage());
    }
}
