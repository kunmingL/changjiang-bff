package com.changjiang.bff.core.introspector;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;

import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.util.Arrays;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 参数名称发现器
 * 主要职责：
 * 1. 解析方法参数的名称信息
 * 2. 支持多种参数名称获取策略
 * 3. 提供参数名称缓存机制
 * 
 * 使用场景：
 * - 方法参数的名称解析
 * - API文档生成
 * - 参数绑定处理
 * 
 * 调用关系：
 * - 被MethodIntrospector调用
 * - 与MethodParameter配合使用
 * - 支持Spring的参数解析机制
 */
@Component
public class ParameterNameDiscoverer {
    private static final Logger logger = LoggerFactory.getLogger(ParameterNameDiscoverer.class);
    
    /** 
     * 参数名称缓存
     * 存储已解析的参数名称
     * key: 方法签名, value: 参数名称数组
     */
    private final Map<String, String[]> parameterNamesCache = new ConcurrentHashMap<>();
    
    /**
     * 获取方法参数名称
     */
    public String[] getParameterNames(Method method) {
        String key = generateMethodKey(method);
        return parameterNamesCache.computeIfAbsent(key, k -> discoverParameterNames(method));
    }
    
    /**
     * 发现参数名称
     */
    private String[] discoverParameterNames(Method method) {
        Parameter[] parameters = method.getParameters();
        String[] parameterNames = new String[parameters.length];
        
        for (int i = 0; i < parameters.length; i++) {
            Parameter parameter = parameters[i];
            
            // 尝试从@RequestParam注解获取
            RequestParam requestParam = parameter.getAnnotation(RequestParam.class);
            if (requestParam != null && !requestParam.value().isEmpty()) {
                parameterNames[i] = requestParam.value();
                continue;
            }
            
            // 尝试从@PathVariable注解获取
            PathVariable pathVariable = parameter.getAnnotation(PathVariable.class);
            if (pathVariable != null && !pathVariable.value().isEmpty()) {
                parameterNames[i] = pathVariable.value();
                continue;
            }
            
            // 尝试获取参数名称
            if (parameter.isNamePresent()) {
                parameterNames[i] = parameter.getName();
            } else {
                // 使用默认命名
                parameterNames[i] = "arg" + i;
                logger.warn("Parameter name not found for parameter {} in method {}", i, method);
            }
        }
        
        return parameterNames;
    }
    
    /**
     * 生成方法唯一键
     */
    private String generateMethodKey(Method method) {
        return method.getDeclaringClass().getName() + "#" + method.getName() + 
               Arrays.toString(method.getParameterTypes());
    }
} 