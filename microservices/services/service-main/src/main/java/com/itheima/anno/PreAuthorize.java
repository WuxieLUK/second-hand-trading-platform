package com.itheima.anno;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import static java.lang.annotation.RetentionPolicy.RUNTIME;

@Documented//元注解
@Target(ElementType.METHOD)//元注解
@Retention(RUNTIME)//元注解
public @interface PreAuthorize {
    /*权限标识符*/
    public String value() default "";

}
