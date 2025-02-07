package com.changjing.bff.common;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Documented
@Target({ElementType.METHOD, ElementType.ANNOTATION_TYPE})
@Retention(RetentionPolicy.RUNTIME)
public @interface ServiceConfig {
    String registryId() default "";
    String url() default "";
    SrvChannel[] channel() default {SrvChannel.PC};
    boolean dealResType() default false;
    boolean dataMask() default false;
    String methodName() default "";
    SpecClassReference[] specClassReference() default {};
    String referField() default "";
}
