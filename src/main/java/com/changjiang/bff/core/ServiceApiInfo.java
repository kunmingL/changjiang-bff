package com.changjiang.bff.core;

import com.changjiang.bff.annotation.ServiceConfig;
import com.changjiang.bff.core.introspector.MethodParameterHandler;
import com.changjiang.bff.util.NpcsDataMaskUtil;
import lombok.Builder;
import lombok.Data;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.List;
import java.util.Map;


/**
 * 服务API信息类
 * 主要职责：
 * 1. 存储API的详细信息
 * 2. 提供方法调用的必要信息
 * 3. 支持API文档生成
 */
@Data
@Builder
public class ServiceApiInfo {
    /** 
     * 服务实例
     * 用于方法调用
     */
    private Object instance;
    
    /** 
     * 方法对象
     * 存储方法的反射信息
     */
    private Method method;
    
    /** 
     * 参数处理器
     * 用于处理方法参数
     */
    private MethodParameterHandler parameterHandler;
    
    /** 
     * API描述
     * 用于文档生成
     */
    private String description;

    private Class requestType;

    private Class responseType;

    private String methodName;

    private String url;

    private String registryId;

    
    /** 
     * 参数描述列表
     * 用于参数说明
     */
    private List<ParameterDescription> parameters;
    
    /** 
     * 返回值描述
     * 用于返回值说明
     */
    private ReturnDescription returnDesc;
    
    /** 是否需要验证 */
    private boolean needValidate;
    
    /** 是否需要脱敏 */
    private boolean needMask;
    
    /** 特殊类型引用映射 */
    private Map<String, Class<?>> specClassReferMap;
    
    /** 引用字段名 */
    private String referField;

    private ServiceConfig serviceConfig;
    
    /**
     * 执行方法调用
     */
    public Object invoke(Map<String, Object> params) throws Exception {
        try {
            // 参数验证
            if (needValidate) {
                validateParameters(params);
            }
            
            // 处理方法参数
            Object[] args = parameterHandler.handleMethodParameters(method, params);
            
            // 执行方法调用
            Object result = method.invoke(instance, args);
            
            // 处理返回值脱敏
            if (needMask && result != null) {
                result = NpcsDataMaskUtil.doDataMask(result);
            }
            
            return result;
            
        } catch (InvocationTargetException e) {
           throw e;
        }
    }
    
    /**
     * 验证参数
     */
    private void validateParameters(Map<String, Object> params) {
        if (parameters == null || parameters.isEmpty()) {
            return;
        }
        
        for (ParameterDescription param : parameters) {
            Object value = params.get(param.getName());
            
            // 必填参数验证
            if (param.isRequired() && value == null) {
                throw new IllegalArgumentException("Required parameter '" + param.getName() + "' is missing");
            }
            
            // 验证规则检查
            if (value != null && param.getValidations() != null) {
                for (String validation : param.getValidations()) {
                    validateParameter(param.getName(), value, validation);
                }
            }
        }
    }
    
    /**
     * 执行参数验证规则
     */
    private void validateParameter(String name, Object value, String validation) {
        if (validation == null || validation.isEmpty()) {
            return;
        }
        
        // 解析验证规则
        String[] parts = validation.split(":");
        String rule = parts[0];
        String param = parts.length > 1 ? parts[1] : null;
        
        switch (rule.toLowerCase()) {
            case "length":
                validateLength(name, value.toString(), param);
                break;
            case "range":
                validateRange(name, value.toString(), param);
                break;
            case "pattern":
                validatePattern(name, value.toString(), param);
                break;
            default:
                //logger.warn("Unknown validation rule: {}", rule);
        }
    }
    
    /**
     * 验证字符串长度
     */
    private void validateLength(String name, String value, String param) {
        if (param == null) {
            return;
        }
        
        String[] limits = param.split(",");
        int min = Integer.parseInt(limits[0]);
        int max = limits.length > 1 ? Integer.parseInt(limits[1]) : Integer.MAX_VALUE;
        
        if (value.length() < min || value.length() > max) {
            throw new IllegalArgumentException(
                String.format("Parameter '%s' length must be between %d and %d", name, min, max));
        }
    }
    
    /**
     * 验证数值范围
     */
    private void validateRange(String name, String value, String param) {
        if (param == null) {
            return;
        }
        
        String[] limits = param.split(",");
        double min = Double.parseDouble(limits[0]);
        double max = Double.parseDouble(limits[1]);
        double num = Double.parseDouble(value);
        
        if (num < min || num > max) {
            throw new IllegalArgumentException(
                String.format("Parameter '%s' must be between %f and %f", name, min, max));
        }
    }
    
    /**
     * 验证正则表达式
     */
    private void validatePattern(String name, String value, String pattern) {
        if (pattern == null || !value.matches(pattern)) {
            throw new IllegalArgumentException(
                String.format("Parameter '%s' does not match required pattern", name));
        }
    }
} 