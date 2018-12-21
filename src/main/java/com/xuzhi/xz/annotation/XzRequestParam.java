package com.xuzhi.xz.annotation;

import java.lang.annotation.*;

@Target(ElementType.PARAMETER)//作用范围：参数
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface XzRequestParam {
    String value() default "";
}
