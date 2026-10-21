package com.lmf.heritage.module.coupon.controller;

import com.lmf.heritage.annotation.LoginRequired;
import com.lmf.heritage.annotation.RateLimit;
import com.lmf.heritage.common.PageResult;
import com.lmf.heritage.common.Result;
import com.lmf.heritage.module.coupon.dto.CouponQueryDTO;
import com.lmf.heritage.module.coupon.service.CouponService;
import com.lmf.heritage.module.coupon.vo.CouponAvailableResultVO;
import com.lmf.heritage.module.coupon.vo.CouponVO;
import com.lmf.heritage.module.coupon.vo.UserCouponVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.util.Map;

/**
 * 用户端 · 优惠券
 *
 * <p>抵扣金额一律由后端算好返回（{@code discountAmount} / {@code payAmount}），
 * 前端只负责展示。<b>前端算一遍、后端再算一遍是电商里最经典的坑</b>——
 * 两边的舍入方式只要差一点，就会出现「结算页显示 84.99、实付 85.00」的对不上账。</p>
 *
 * @author 李明峰 23100025
 */
@Tag(name = "用户端-优惠券", description = "领券中心 / 领取 / 我的券 / 结算可用券")
@RestController
@RequestMapping("/portal/coupon")
@RequiredArgsConstructor
@LoginRequired
public class PortalCouponController {

    private final CouponService couponService;

    @Operation(summary = "领券中心",
            description = "返回启用中且在有效期内的券（含已抢光的），并给出当前用户「已领几张 / 还能不能领 / 不能领的原因」")
    @GetMapping("/center")
    public Result<PageResult<CouponVO>> center(CouponQueryDTO query) {
        return Result.success(couponService.center(query));
    }

    @Operation(summary = "领取优惠券",
            description = "校验发放总量（原子扣减）与每人限领（分布式锁串行化），成功后写入卡包并发站内消息")
    @RateLimit(count = 10, window = 60, dimension = "user", message = "领取过于频繁，请稍后再试")
    @PostMapping("/receive/{couponId}")
    public Result<Void> receive(@PathVariable("couponId") Long couponId) {
        couponService.receive(couponId);
        return Result.success();
    }

    @Operation(summary = "我的优惠券", description = "status 0未使用 1已使用 2已过期，不传为全部")
    @GetMapping("/mine")
    public Result<PageResult<UserCouponVO>> mine(CouponQueryDTO query) {
        return Result.success(couponService.mine(query));
    }

    @Operation(summary = "我的优惠券数量统计",
            description = "返回 {0:未使用数, 1:已使用数, 2:已过期数}，用于我的券三个 tab 的角标")
    @GetMapping("/mine/count")
    public Result<Map<Integer, Integer>> myCount() {
        return Result.success(couponService.myCount());
    }

    @Operation(summary = "结算页可用券",
            description = "传 order 金额 amount 与消费类型 scope（1商品 2课程），"
                    + "返回可用券列表（按抵扣金额降序）、每张券的抵扣额与抵扣后应付金额，"
                    + "以及最优券与最优抵扣后金额")
    @GetMapping("/available")
    public Result<CouponAvailableResultVO> available(
            @RequestParam("amount") BigDecimal amount,
            @RequestParam(value = "scope", defaultValue = "1") Integer scope) {
        return Result.success(couponService.available(amount, scope));
    }
}
