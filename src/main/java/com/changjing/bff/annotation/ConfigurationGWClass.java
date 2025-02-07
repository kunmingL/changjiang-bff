package com.changjing.bff.annotation;

import java.lang.annotation.*;

@Documented
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
public @interface ConfigurationGWClass {

    String value() default "";

    String reqUrlPrefix() default "";
}
