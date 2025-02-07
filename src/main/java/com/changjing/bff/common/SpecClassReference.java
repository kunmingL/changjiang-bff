package com.changjing.bff.common;

import java.lang.annotation.*;

@Documented
@Target({ElementType.METHOD,ElementType.ANNOTATION_TYPE})
@Retention(RetentionPolicy.RUNTIME)
public @interface SpecClassReference {

    String field();

    Class<?> targetClass();

}
