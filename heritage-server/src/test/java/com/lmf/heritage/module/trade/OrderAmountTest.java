package com.lmf.heritage.module.trade;

import com.lmf.heritage.common.BusinessException;
import com.lmf.heritage.module.coupon.entity.Coupon;
import com.lmf.heritage.module.coupon.strategy.CouponStrategyFactory;
import com.lmf.heritage.module.coupon.strategy.impl.DiscountStrategy;
import com.lmf.heritage.module.coupon.strategy.impl.FullReductionStrategy;
import com.lmf.heritage.module.trade.enums.OrderStatusEnum;
import com.lmf.heritage.module.trade.vo.CartItemVO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 订单金额计算单元测试
 *
 * <p>复现 OrderServiceImpl.preview 的金额口径（纯计算部分）：</p>
 * <ol>
 *   <li>单项小计 = 单价 × 数量（CartItemVO.calcSubtotal，HALF_UP 保留 2 位）；</li>
 *   <li>订单总额 = 各有效项小计之和；</li>
 *   <li>实付金额 = 总额 − 券抵扣（券抵扣由策略工厂按券型路由计算）；</li>
 *   <li>库存不足的项标记失效（invalid=1），不可结算。</li>
 * </ol>
 * <p>另附订单状态机的合法/非法流转断言（与金额一起构成下单闭环的守护）。</p>
 */
class OrderAmountTest {

    private CouponStrategyFactory factory;

    @BeforeEach
    void setUp() {
        factory = new CouponStrategyFactory(List.of(new FullReductionStrategy(), new DiscountStrategy()));
    }

    private CartItemVO item(long goodsId, String price, int quantity, int stock) {
        CartItemVO i = new CartItemVO();
        i.setGoodsId(goodsId);
        i.setPrice(new BigDecimal(price));
        i.setQuantity(quantity);
        i.setStock(stock);
        // 与 OrderServiceImpl.loadPendingItem 相同的失效判定
        i.setInvalid(i.getStock() < i.getQuantity() ? 1 : 0);
        i.calcSubtotal();
        return i;
    }

    private BigDecimal total(List<CartItemVO> items) {
        return items.stream()
                .map(i -> i.getSubtotal() == null ? BigDecimal.ZERO : i.getSubtotal())
                .reduce(BigDecimal.ZERO, BigDecimal::add)
                .setScale(2, RoundingMode.HALF_UP);
    }

    private Coupon coupon(int type, String threshold, String amount, String rate) {
        Coupon c = new Coupon();
        c.setId(1L);
        c.setType(type);
        c.setThreshold(threshold == null ? null : new BigDecimal(threshold));
        c.setAmount(amount == null ? null : new BigDecimal(amount));
        c.setDiscount(rate == null ? null : new BigDecimal(rate));
        return c;
    }

    /* ============ 单项小计 ============ */

    @Test
    @DisplayName("小计 = 单价 × 数量，HALF_UP 保留 2 位")
    void subtotal() {
        assertEquals(new BigDecimal("299.00"), item(1, "299.00", 1, 10).getSubtotal());
        assertEquals(new BigDecimal("596.00"), item(2, "149.00", 4, 20).getSubtotal());
        assertEquals(new BigDecimal("16.67"), item(3, "5.555", 3, 10).getSubtotal(),
                "5.555×3=16.665 → HALF_UP 进位到 16.67");
        assertEquals(new BigDecimal("7.76"), item(4, "2.585", 3, 10).getSubtotal(),
                "2.585×3=7.755 → HALF_UP 进位到 7.76");
    }

    @Test
    @DisplayName("单价或数量为 null 时小计保持 null（不猜 0，交由汇总判空）")
    void subtotalNull() {
        CartItemVO i = new CartItemVO();
        i.setPrice(null);
        i.setQuantity(2);
        i.calcSubtotal();
        assertNull(i.getSubtotal());
    }

    /* ============ 失效判定 ============ */

    @Test
    @DisplayName("库存不足的项标记 invalid=1，不可结算")
    void invalidItem() {
        CartItemVO ok = item(1, "100", 2, 5);
        CartItemVO bad = item(2, "100", 6, 5);
        assertEquals(0, ok.getInvalid());
        assertEquals(1, bad.getInvalid());
    }

    /* ============ 总额与实付 ============ */

    @Test
    @DisplayName("无券：实付 = 总额 = 各项小计之和")
    void totalWithoutCoupon() {
        List<CartItemVO> items = List.of(
                item(1, "299.00", 1, 10),
                item(2, "149.00", 2, 10),
                item(3, "55.55", 3, 10));
        BigDecimal total = total(items);
        assertEquals(new BigDecimal("763.65"), total);
        // 无券时 discountAmount=0，payAmount = total - 0
        BigDecimal pay = total.subtract(new BigDecimal("0.00").setScale(2, RoundingMode.HALF_UP));
        assertEquals(total, pay);
    }

    @Test
    @DisplayName("用满减券：总额 763.65 减 50 → 实付 713.65")
    void payWithFullReduction() {
        List<CartItemVO> items = List.of(
                item(1, "299.00", 1, 10),
                item(2, "149.00", 2, 10),
                item(3, "55.55", 3, 10));
        BigDecimal total = total(items);

        BigDecimal discount = factory.calculateDiscount(coupon(1, "500", "50", null), total);
        assertEquals(new BigDecimal("50.00"), discount);

        BigDecimal pay = total.subtract(discount).setScale(2, RoundingMode.HALF_UP);
        assertEquals(new BigDecimal("713.65"), pay);
        assertTrue(pay.compareTo(BigDecimal.ZERO) > 0, "实付必须为正");
    }

    @Test
    @DisplayName("用折扣券：总额 200 × (1−0.85) → 抵扣 30.00 → 实付 170.00")
    void payWithDiscountCoupon() {
        BigDecimal total = total(List.of(item(1, "100.00", 2, 10)));
        BigDecimal discount = factory.calculateDiscount(coupon(2, "100", null, "0.85"), total);
        assertEquals(new BigDecimal("30.00"), discount);
        assertEquals(new BigDecimal("170.00"), total.subtract(discount).setScale(2, RoundingMode.HALF_UP));
    }

    @Test
    @DisplayName("抵扣封顶：总额 20 用满 100 减 30 的券 → 抵扣 20，实付 0 而不是负数")
    void payNeverNegative() {
        BigDecimal total = total(List.of(item(1, "20.00", 1, 10)));
        BigDecimal discount = factory.calculateDiscount(coupon(1, "100", "30", null), total);
        assertEquals(new BigDecimal("20.00"), discount);
        assertEquals(new BigDecimal("0.00"), total.subtract(discount).setScale(2, RoundingMode.HALF_UP));
    }

    /* ============ 订单状态机（下单闭环守护） ============ */

    @Test
    @DisplayName("订单合法流转：支付→发货→收货→完成 / 待付款取消 / 待发货收货退款")
    void orderLegalTransfers() {
        assertDoesNotThrow(() -> OrderStatusEnum.checkTransfer(0, OrderStatusEnum.WAIT_DELIVER)); // 支付
        assertDoesNotThrow(() -> OrderStatusEnum.checkTransfer(0, OrderStatusEnum.CANCELED));     // 取消
        assertDoesNotThrow(() -> OrderStatusEnum.checkTransfer(1, OrderStatusEnum.WAIT_RECEIVE)); // 发货
        assertDoesNotThrow(() -> OrderStatusEnum.checkTransfer(1, OrderStatusEnum.REFUNDED));     // 退款
        assertDoesNotThrow(() -> OrderStatusEnum.checkTransfer(2, OrderStatusEnum.FINISHED));     // 收货
        assertDoesNotThrow(() -> OrderStatusEnum.checkTransfer(2, OrderStatusEnum.REFUNDED));     // 退款
    }

    @Test
    @DisplayName("订单非法流转：待付款不能直接发货完成/退款，终态不能再流转")
    void orderIllegalTransfers() {
        assertThrows(BusinessException.class, () -> OrderStatusEnum.checkTransfer(0, OrderStatusEnum.WAIT_RECEIVE));
        assertThrows(BusinessException.class, () -> OrderStatusEnum.checkTransfer(0, OrderStatusEnum.FINISHED));
        assertThrows(BusinessException.class, () -> OrderStatusEnum.checkTransfer(0, OrderStatusEnum.REFUNDED));
        assertThrows(BusinessException.class, () -> OrderStatusEnum.checkTransfer(1, OrderStatusEnum.WAIT_DELIVER));
        assertThrows(BusinessException.class, () -> OrderStatusEnum.checkTransfer(3, OrderStatusEnum.REFUNDED));
        assertThrows(BusinessException.class, () -> OrderStatusEnum.checkTransfer(5, OrderStatusEnum.WAIT_PAY));
        assertThrows(BusinessException.class, () -> OrderStatusEnum.checkTransfer(null, OrderStatusEnum.WAIT_PAY));
        assertThrows(BusinessException.class, () -> OrderStatusEnum.of(99));
    }
}
