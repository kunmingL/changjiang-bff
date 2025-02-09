package com.changjiang.bff.annotation;

import com.changjiang.bff.enums.MaskType;
import java.lang.annotation.*;

/**
 * 数据脱敏注解
 * 主要职责：
 * 1. 标记需要脱敏的字段
 * 2. 指定脱敏规则
 * 3. 支持自定义脱敏策略
 */
@Target(ElementType.FIELD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface DataMask {
    /**
     * 脱敏类型
     */
    MaskType type() default MaskType.DEFAULT;
    
    /**
     * 自定义脱敏规则
     */
    String pattern() default "";
} 