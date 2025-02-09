package com.changjiang.bff.annotation;

import java.lang.annotation.*;

/**
 * 网关配置类注解
 * 主要职责：
 * 1. 标记网关配置类
 * 2. 提供URL前缀配置
 * 3. 支持网关路由的统一管理
 * 
 * 使用场景：
 * - 标注网关配置类
 * - 定义服务组的URL前缀
 * - 配置网关路由规则
 * 
 * 调用关系：
 * - 被MethodIntrospector扫描并解析
 * - 被路由系统用于URL匹配
 * - 被日志系统用于请求追踪
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface ConfigurationGWClass {
    /** 
     * 请求URL前缀
     * 定义服务组的统一前缀
     * 被路由系统用于URL匹配
     */
    String reqUrlPrefix() default "";
    
    /** 
     * 配置描述
     * 说明配置类的用途
     * 用于文档生成
     */
    String description() default "";
}
