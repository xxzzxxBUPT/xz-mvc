package com.xuzhi.xz.annotation;

import java.lang.annotation.*;

@Target(ElementType.FIELD)//作用范围：成员变量
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface XzAutowired {
    String value() default "";
}
