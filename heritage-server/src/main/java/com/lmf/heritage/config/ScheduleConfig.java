package com.lmf.heritage.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * 定时任务开关
 *
 * <p>目前开启的定时任务（见 {@code com.lmf.heritage.task}）：</p>
 * <ul>
 *   <li>浏览量回写：每小时把 Redis 计数器同步到 MySQL</li>
 * </ul>
 *
 * <p>P5 会继续加：预约审核超时自动处理、课程结束后自动完成预约、开课提醒。</p>
 *
 * @author 李明峰 23100025
 */
@Configuration
@EnableScheduling
public class ScheduleConfig {
}
