package com.changjiang.bff.annotation;

import java.lang.annotation.*;

/**
 * 方法配置注解
 * 主要职责：
 * 1. 标记需要配置的方法
 * 2. 提供方法级别的路由配置
 * 3. 支持方法调用的参数验证
 * 
 * 使用场景：
 * - 标注服务方法
 * - 配置方法的路由规则
 * - 定义方法的调用参数
 * 
 * 调用关系：
 * - 被MethodIntrospector扫描和解析
 * - 被ServiceApiScanner用于方法注册
 * - 被TransferService用于方法调用
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface ConfigureMethod {
    /** 
     * 请求URL
     * 定义方法的访问路径
     * 被路由系统用于请求转发
     */
    String reqUrl() default "";
    
    /** 
     * 方法描述
     * 说明方法的功能
     * 用于API文档生成
     */
    String description() default "";
    
    /** 
     * 是否需要认证
     * 控制方法的访问权限
     * 被安全系统用于权限校验
     */
    boolean requireAuth() default true;
}
