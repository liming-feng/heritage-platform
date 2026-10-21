package com.lmf.heritage.module.coupon.strategy;

import com.lmf.heritage.common.BusinessException;
import com.lmf.heritage.common.ErrorCode;
import com.lmf.heritage.module.coupon.entity.Coupon;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * <b>抵扣策略工厂</b>：按 {@code coupon.type} 把计算派发给对应策略
 *
 * <p>Spring 会把所有 {@link CouponStrategy} 的实现类注入到构造参数里的 {@code List} 中，
 * 工厂只做一件事：把 `type → 策略` 建成一张 Map。
 * 因此<b>新增券型不需要改动任何既有代码</b>——写一个新的 {@code @Component} 实现即可，
 * 它会自动出现在这张表里。</p>
 *
 * <p>构造时对「同一个 type 被两个策略注册」做了<b>快速失败</b>：
 * 这种配置错误如果拖到运行期才暴露，表现是「偶尔算错钱」，
 * 比启动时直接报错难排查得多。</p>
 *
 * @author 李明峰 23100025
 */
@Slf4j
@Component
public class CouponStrategyFactory {

    private final Map<Integer, CouponStrategy> strategies;

    public CouponStrategyFactory(List<CouponStrategy> strategyList) {
        Map<Integer, CouponStrategy> map = new HashMap<>();
        for (CouponStrategy strategy : strategyList) {
            CouponStrategy exists = map.put(strategy.supportType(), strategy);
            if (exists != null) {
                throw new IllegalStateException(String.format(
                        "优惠券策略重复注册：type=%d 同时被 %s 与 %s 声明",
                        strategy.supportType(), exists.getClass().getSimpleName(),
                        strategy.getClass().getSimpleName()));
            }
        }
        this.strategies = Map.copyOf(map);
        log.info("[优惠券] 策略注册完成，共 {} 种：{}", map.size(), map.keySet());
    }

    /** 取券型对应的策略；券型未注册时抛出业务异常（通常是数据库里的 type 值非法） */
    public CouponStrategy of(Integer type) {
        CouponStrategy strategy = type == null ? null : strategies.get(type);
        if (strategy == null) {
            throw new BusinessException(ErrorCode.COUPON_NOT_FOUND, "不支持的优惠券类型：" + type);
        }
        return strategy;
    }

    /** 便捷方法：直接算抵扣金额（调用方无需先取策略） */
    public BigDecimal calculateDiscount(Coupon coupon, BigDecimal amount) {
        if (coupon == null) {
            return BigDecimal.ZERO.setScale(2, java.math.RoundingMode.HALF_UP);
        }
        return of(coupon.getType()).calculateDiscount(coupon, amount);
    }

    /** 便捷方法：判断是否满足门槛 */
    public boolean meetThreshold(Coupon coupon, BigDecimal amount) {
        if (coupon == null) {
            return false;
        }
        return of(coupon.getType()).meetThreshold(coupon, amount);
    }

    /** 便捷方法：生成展示文案 */
    public String describe(Coupon coupon) {
        if (coupon == null) {
            return "优惠券";
        }
        CouponStrategy strategy = strategies.get(coupon.getType());
        // 文案属于展示层，脏数据时降级而不是抛异常，避免整个列表接口挂掉
        return strategy == null ? "优惠券" : strategy.describe(coupon);
    }
}
