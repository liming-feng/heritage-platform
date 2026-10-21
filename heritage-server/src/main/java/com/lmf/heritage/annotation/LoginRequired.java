package com.lmf.heritage.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 标注在需要登录才能访问的方法或类上。
 *
 * <p>拦截器只对带该注解的接口做鉴权，未标注的接口默认放行（可选登录场景由业务自行判断）。</p>
 */
@Target({ElementType.METHOD, ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
public @interface LoginRequired {

    /** 允许访问的角色，默认不限制（只要登录即可） */
    int[] roles() default {};
}
