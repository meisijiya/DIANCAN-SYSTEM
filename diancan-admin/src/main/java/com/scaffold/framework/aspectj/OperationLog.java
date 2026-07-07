package com.scaffold.framework.aspectj;

import java.lang.annotation.*;

/**
 * 操作日志注解
 *
 * @author Henfon
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface OperationLog {

    /**
     * 模块名称
     */
    String module() default "";

    /**
     * 操作描述
     */
    String operation() default "";
}
