package com.lmf.heritage.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 操作日志注解：标注的方法会被 OperationLogAspect 环绕，
 * 采集操作人、角色、操作描述、方法签名、请求参数、IP、耗时，异步入库。
 *
 * <pre>
 * &#64;OperationLog("审核预约")
 * </pre>
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface OperationLog {

    /** 操作描述，如「新增商品」「审核预约」 */
    String value();

    /** 是否记录请求参数 */
    boolean saveParams() default true;
}
