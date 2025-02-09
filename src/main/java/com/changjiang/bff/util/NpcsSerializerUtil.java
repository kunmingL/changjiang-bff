package com.changjiang.bff.util;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import com.changjiang.bff.exception.ServiceException;

import java.util.Map;

/**
 * 序列化工具类
 * 主要职责：
 * 1. 提供对象序列化和反序列化功能
 * 2. 支持复杂类型的转换
 * 3. 处理泛型类型
 */
@Component
public class NpcsSerializerUtil {
    private static final Logger logger = LoggerFactory.getLogger(NpcsSerializerUtil.class);
    
    private static ObjectMapper objectMapper;
    
    @Autowired
    public void setObjectMapper(ObjectMapper objectMapper) {
        NpcsSerializerUtil.objectMapper = objectMapper;
    }
    
    /**
     * 将对象转换为JSON字符串
     */
    public static String writeValueAsStringNormal(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (Exception e) {
            logger.error("Serialize error", e);
            throw new ServiceException("SERIALIZE_ERROR", "序列化异常", e);
        }
    }
    
    /**
     * 将对象转换为JSON字符串(支持JavaType)
     */
    public static String writeValueAsStringNormal(Object value, JavaType javaType) {
        try {
            return objectMapper.writerFor(javaType).writeValueAsString(value);
        } catch (Exception e) {
            logger.error("Serialize error", e);
            throw new ServiceException("SERIALIZE_ERROR", "序列化异常", e);
        }
    }
    
    /**
     * 将JSON字符串转换为对象
     */
    public static <T> T readValueNormal(String content, Class<T> valueType) {
        try {
            return objectMapper.readValue(content, valueType);
        } catch (Exception e) {
            logger.error("Deserialize error", e);
            throw new ServiceException("DESERIALIZE_ERROR", "反序列化异常", e);
        }
    }
    
    /**
     * 将JSON字符串转换为对象(支持JavaType)
     */
    public static <T> T readValueNormal(String content, JavaType javaType) {
        try {
            return objectMapper.readValue(content, javaType);
        } catch (Exception e) {
            logger.error("Deserialize error", e);
            throw new ServiceException("DESERIALIZE_ERROR", "反序列化异常", e);
        }
    }
    
    /**
     * 获取JavaType
     */
    public static JavaType getTypeByClass(Class<?> clazz, Class<?>... parameterClasses) {
        return objectMapper.getTypeFactory().constructParametricType(clazz, parameterClasses);
    }
    
    /**
     * 将对象转换为Map
     */
    public static Map<String, Object> convertToMap(Object obj) {
        return objectMapper.convertValue(obj, new TypeReference<Map<String, Object>>() {});
    }
    
    /**
     * 将值转换为指定类型
     */
    public static <T> T convertValue(Object fromValue, Class<T> toValueType) {
        return objectMapper.convertValue(fromValue, toValueType);
    }
} 