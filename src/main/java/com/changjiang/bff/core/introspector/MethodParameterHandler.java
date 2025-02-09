package com.changjiang.bff.core.introspector;
import com.changjiang.bff.util.NpcsSerializerUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

import org.springframework.expression.ParseException;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.annotation.RequestParam;

import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.text.SimpleDateFormat;
import java.util.Collection;
import java.util.Date;
import java.util.Map;

/**
 * 方法参数处理器
 * 主要职责：
 * 1. 处理方法参数的类型转换
 * 2. 验证参数的有效性
 * 3. 支持复杂参数的解析
 */
@Component
public class MethodParameterHandler {
    private static final Logger logger = LoggerFactory.getLogger(MethodParameterHandler.class);
    
    @Autowired
    private ParameterNameDiscoverer parameterNameDiscoverer;
    
    /**
     * 处理方法参数
     */
    public Object[] handleMethodParameters(Method method, Map<String, Object> params) {
        Parameter[] parameters = method.getParameters();
        String[] parameterNames = parameterNameDiscoverer.getParameterNames(method);
        Object[] args = new Object[parameters.length];
        
        for (int i = 0; i < parameters.length; i++) {
            Parameter parameter = parameters[i];
            String paramName = parameterNames[i];
            Class<?> paramType = parameter.getType();
            
            // 获取参数值
            Object paramValue = params.get(paramName);
            if (paramValue == null && parameter.isAnnotationPresent(RequestParam.class)) {
                RequestParam annotation = parameter.getAnnotation(RequestParam.class);
                if (annotation.required()) {
                    throw new IllegalArgumentException("Required parameter '" + paramName + "' is missing");
                }
                paramValue = annotation.defaultValue();
            }
            
            // 转换参数类型
            args[i] = convertParameterType(paramValue, paramType);
        }
        
        return args;
    }
    
    /**
     * 转换参数类型
     */
    private Object convertParameterType(Object value, Class<?> targetType) {
        if (value == null) {
            return null;
        }
        
        try {
            // 处理基本类型
            if (targetType.isPrimitive()) {
                return handlePrimitiveType(value, targetType);
            }
            
            // 处理集合类型
            if (Collection.class.isAssignableFrom(targetType)) {
                return handleCollectionType(value, targetType);
            }
            
            // 处理Map类型
            if (Map.class.isAssignableFrom(targetType)) {
                return handleMapType(value, targetType);
            }
            
            // 处理自定义对象类型
            return NpcsSerializerUtil.convertValue(value, targetType);
            
        } catch (Exception e) {
            logger.error("Parameter type conversion error", e);
            throw new IllegalArgumentException("Failed to convert parameter to type: " + targetType.getName());
        }
    }
    
    /**
     * 处理基本类型转换
     */
    private Object handlePrimitiveType(Object value, Class<?> targetType) {
        if (value == null) {
            if (targetType.isPrimitive()) {
                throw new IllegalArgumentException("Cannot convert null to primitive type: " + targetType);
            }
            return null;
        }

        String strValue = value.toString().trim();
        
        try {
            if (targetType == int.class || targetType == Integer.class) {
                return Integer.valueOf(strValue);
            }
            if (targetType == long.class || targetType == Long.class) {
                return Long.valueOf(strValue);
            }
            if (targetType == double.class || targetType == Double.class) {
                return Double.valueOf(strValue);
            }
            if (targetType == float.class || targetType == Float.class) {
                return Float.valueOf(strValue);
            }
            if (targetType == boolean.class || targetType == Boolean.class) {
                return Boolean.valueOf(strValue);
            }
            if (targetType == byte.class || targetType == Byte.class) {
                return Byte.valueOf(strValue);
            }
            if (targetType == short.class || targetType == Short.class) {
                return Short.valueOf(strValue);
            }
            if (targetType == char.class || targetType == Character.class) {
                if (strValue.length() != 1) {
                    throw new IllegalArgumentException("Cannot convert to char: " + strValue);
                }
                return strValue.charAt(0);
            }
            
            // 处理日期类型
            if (targetType == Date.class) {
                return parseDate(strValue);
            }
            
            // 处理枚举类型
            if (targetType.isEnum()) {
                return Enum.valueOf((Class<? extends Enum>) targetType, strValue);
            }
            
        } catch (Exception e) {
            throw new IllegalArgumentException(
                "Failed to convert value '" + strValue + "' to type " + targetType.getName(), e);
        }
        
        return value;
    }
    
    /**
     * 解析日期字符串
     */
    private Date parseDate(String dateStr) {
        try {
            // 尝试多种日期格式
            String[] patterns = {
                "yyyy-MM-dd HH:mm:ss",
                "yyyy-MM-dd",
                "yyyy/MM/dd HH:mm:ss",
                "yyyy/MM/dd"
            };
            
            for (String pattern : patterns) {
                try {
                    return new SimpleDateFormat(pattern).parse(dateStr);
                } catch (ParseException ignored) {
                    // 继续尝试下一个格式
                }
            }
            
            // 尝试时间戳
            return new Date(Long.parseLong(dateStr));
            
        } catch (Exception e) {
            throw new IllegalArgumentException("Invalid date format: " + dateStr);
        }
    }
    
    /**
     * 处理集合类型转换
     */
    private Collection<?> handleCollectionType(Object value, Class<?> targetType) {
        if (value instanceof Collection) {
            return (Collection<?>) value;
        }
        if (value instanceof String) {
            //return NpcsSerializerUtil.readValue((String) value, targetType);
        }
        throw new IllegalArgumentException("Cannot convert to collection type: " + targetType.getName());
    }
    
    /**
     * 处理Map类型转换
     */
    private Map<?, ?> handleMapType(Object value, Class<?> targetType) {
        if (value instanceof Map) {
            return (Map<?, ?>) value;
        }
        if (value instanceof String) {
            //return NpcsSerializerUtil.readValue((String) value, targetType);
        }
        throw new IllegalArgumentException("Cannot convert to map type: " + targetType.getName());
    }
} 