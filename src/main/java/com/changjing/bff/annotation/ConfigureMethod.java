package com.changjing.bff.annotation;

import java.lang.annotation.*;

@Documented
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface ConfigureMethod {

    String reqUrl() default "";

    String methodName() default "";

    String inputClazzName () default "";
}
