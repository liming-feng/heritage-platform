package com.lmf.heritage.task;

import com.lmf.heritage.module.coupon.service.CouponService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * 优惠券定时任务
 *
 * <p>两件事，都是「不主动做就会一直错下去」的：</p>
 * <ol>
 *   <li><b>过期标记</b>——把过了有效期的未使用券置为「已过期」。
 *       结算页其实已经按 {@code expire_time} 过滤过了，但「我的优惠券」里
 *       那张券还显示「未使用」，用户会以为还能用；</li>
 *   <li><b>过期提醒</b>——24 小时内要过期的券提前通知，避免用户「券过期了才知道」。</li>
 * </ol>
 *
 * <p><b>幂等性：</b>过期标记带 {@code status = 0} 条件，已用掉的券不会被误标；
 * 过期提醒靠 message 表「同券同标题只发一条」去重，重复触发不会重复轰炸。</p>
 *
 * <p>两个任务都与预约任务的执行时刻错开（预约任务在整点与 :05/:10 分），
 * 避免同一分钟内多个任务一起抢数据库连接。</p>
 *
 * @author 李明峰 23100025
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class CouponTask {

    private final CouponService couponService;

    /** 每小时第 15 分钟：标记过期券 */
    @Scheduled(cron = "0 15 * * * ?")
    public void markExpired() {
        long start = System.currentTimeMillis();
        try {
            int rows = couponService.markExpired();
            if (rows <= 0) {
                log.debug("[定时任务] 优惠券过期标记：本次无需处理");
            } else {
                log.info("[定时任务] 优惠券过期标记完成：{} 张，耗时 {} ms",
                        rows, System.currentTimeMillis() - start);
            }
        } catch (Exception e) {
            log.error("[定时任务] 优惠券过期标记异常", e);
        }
    }

    /** 每小时第 20 分钟：发「即将过期」提醒 */
    @Scheduled(cron = "0 20 * * * ?")
    public void sendExpireReminders() {
        long start = System.currentTimeMillis();
        try {
            int sent = couponService.sendExpireReminders();
            if (sent <= 0) {
                log.debug("[定时任务] 优惠券过期提醒：本次无需发送");
            } else {
                log.info("[定时任务] 优惠券过期提醒完成：{} 条，耗时 {} ms",
                        sent, System.currentTimeMillis() - start);
            }
        } catch (Exception e) {
            log.error("[定时任务] 优惠券过期提醒异常", e);
        }
    }
}
