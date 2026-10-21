package com.lmf.heritage.task;

import com.lmf.heritage.module.booking.service.BookingService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * 预约相关定时任务（题眼）
 *
 * <p>三个任务分别对应预约生命周期里「没人管就会卡住」的三个环节：</p>
 * <ol>
 *   <li><b>超时未审自动拒绝</b>——匠人忘记审核时，把名额还给用户、并留下通知，
 *       不然用户的名额会被无限期占住；</li>
 *   <li><b>课程状态推进</b>——开课时间到改「进行中」、结束时间到改「已结束」，
 *       并把已核销的预约推进为「已完成」，这样用户才有评价资格；</li>
 *   <li><b>开课提醒</b>——24 小时内开课且已通过的用户，提前收到站内消息。</li>
 * </ol>
 *
 * <p><b>幂等性怎么保证：</b>三个任务都会在同样的时间点被反复触发，
 * 因此每一步的 SQL 都自带「做过就不再重复做」的条件——</p>
 * <ul>
 *   <li>超时拒绝：{@code transferStatus(id, 0, 2)} 带状态前置条件，处理过的行返回 0 行；</li>
 *   <li>课程推进：{@code pushStatus(id, status)} 带 {@code status <> 目标状态} 条件；</li>
 *   <li>预约转已完成：{@code finishCheckedInByCourse} 只更新 {@code status = 4} 的行；</li>
 *   <li>开课提醒：先查 message 表有没有「同一预约 + 同一类型」的消息再发。</li>
 * </ul>
 *
 * <p><b>异常处理：</b>每个任务都包在 try-catch 里。Spring 的调度线程池如果某次执行抛异常，
 * 只是这一次中断；但把异常吞掉并记录日志，能保证「一次失败不影响后续调度」这件事更明确。</p>
 *
 * @author 李明峰 23100025
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class BookingTask {

    private final BookingService bookingService;

    /**
     * 每 10 分钟：扫描超过 {@code audit_timeout_hours} 仍未审核的预约 → 自动拒绝 + 归还名额 + 站内通知
     */
    @Scheduled(cron = "0 */10 * * * ?")
    public void autoRejectTimeout() {
        long start = System.currentTimeMillis();
        try {
            int handled = bookingService.autoRejectTimeout();
            if (handled > 0) {
                log.info("[定时任务] 预约超时未审处理完成：{} 条，耗时 {} ms",
                        handled, System.currentTimeMillis() - start);
            } else {
                log.debug("[定时任务] 预约超时未审：本次无需处理");
            }
        } catch (Exception e) {
            log.error("[定时任务] 预约超时未审处理异常", e);
        }
    }

    /**
     * 每 10 分钟：课程状态推进（未开始 → 进行中 → 已结束）
     *
     * <p>与上一个任务错开 30 秒执行，避免两个任务同时抢数据库资源。</p>
     */
    @Scheduled(cron = "30 */10 * * * ?")
    public void autoFinishCourses() {
        long start = System.currentTimeMillis();
        try {
            int pushed = bookingService.autoFinishCourses();
            if (pushed > 0) {
                log.info("[定时任务] 课程状态推进完成：{} 门，耗时 {} ms",
                        pushed, System.currentTimeMillis() - start);
            } else {
                log.debug("[定时任务] 课程状态推进：本次无需处理");
            }
        } catch (Exception e) {
            log.error("[定时任务] 课程状态推进异常", e);
        }
    }

    /**
     * 每小时第 5 分钟：给 24 小时内开课且已通过的用户发开课提醒
     *
     * <p>幂等靠 message 表去重，所以即使任务被重复触发，用户也只会收到一条提醒。</p>
     */
    @Scheduled(cron = "0 5 * * * ?")
    public void sendCourseReminders() {
        long start = System.currentTimeMillis();
        try {
            int sent = bookingService.sendCourseReminders();
            if (sent > 0) {
                log.info("[定时任务] 开课提醒发送完成：{} 条，耗时 {} ms",
                        sent, System.currentTimeMillis() - start);
            } else {
                log.debug("[定时任务] 开课提醒：本次无需发送");
            }
        } catch (Exception e) {
            log.error("[定时任务] 开课提醒发送异常", e);
        }
    }
}
