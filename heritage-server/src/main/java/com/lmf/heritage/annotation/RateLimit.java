package com.lmf.heritage.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 接口限流（基于 Redis 计数器 + 拦截器实现）
 *
 * <p>用于登录、下单、预约提交等易被刷的接口。</p>
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface RateLimit {

    /** 时间窗口内允许的最大请求次数 */
    int count() default 5;

    /** 时间窗口，单位秒 */
    int window() default 60;

    /** 限流维度：ip / user / global */
    String dimension() default "ip";

    /** 触发限流时的提示 */
    String message() default "操作过于频繁，请稍后再试";
}
