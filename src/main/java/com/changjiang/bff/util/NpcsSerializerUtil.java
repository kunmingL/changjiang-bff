package com.changjiang.bff.util;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONObject;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import com.changjiang.bff.exception.ServiceException;

import javax.annotation.PostConstruct;
import java.util.Map;
import java.util.Collection;
import java.lang.reflect.Method;

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
    
    @PostConstruct
    public void init() {
        objectMapper = new ObjectMapper();
        // 配置ObjectMapper
        objectMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        objectMapper.configure(DeserializationFeature.ACCEPT_EMPTY_STRING_AS_NULL_OBJECT, true);
    }
    
    @Autowired
    public void setObjectMapper(ObjectMapper objectMapper) {
        NpcsSerializerUtil.objectMapper = objectMapper;
    }
    
    /**
     * 将JSONObject转换为指定类型的对象
     */
    public static <T> T convertToEntity(JSONObject jsonObject, Class<T> targetType) {
        try {
            // 1. 尝试使用FastJSON直接转换
            T result = jsonObject.to(targetType);
            if (result != null) {
                return result;
            }
            
            // 2. 尝试使用Jackson转换
            String jsonStr = jsonObject.toString();
            return objectMapper.readValue(jsonStr, targetType);
        } catch (Exception e) {
            logger.error("Convert to entity error, targetType: {}, error: {}", targetType.getName(), e.getMessage());
            throw new ServiceException("CONVERT_ERROR", "实体转换失败", e);
        }
    }
    
    /**
     * 将Map转换为指定类型的对象
     */
    public static <T> T convertMapToEntity(Map<String, Object> map, Class<T> targetType) {
        try {
            return objectMapper.convertValue(map, targetType);
        } catch (Exception e) {
            logger.error("Convert map to entity error", e);
            throw new ServiceException("CONVERT_ERROR", "Map转实体失败", e);
        }
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
     * 将JSON字符串转换为对象，支持动态加载的类
     */
    public static <T> T readValueNormal(String content, Class<T> valueType) {
        try {
            logger.info("开始反序列化, content: {}, targetType: {}", content, valueType.getName());
            
            // 1. 尝试使用FastJSON2直接转换
            try {
                T result = JSON.parseObject(content, valueType);
                if (result != null) {
                    logger.info("FastJSON2转换成功, result: {}", result);
                    return result;
                }
            } catch (Exception e) {
                logger.warn("FastJSON2转换失败，尝试其他方式: {}", e.getMessage());
            }
            
            // 2. 尝试使用Jackson转换
            try {
                T result = objectMapper.readValue(content, valueType);
                logger.info("Jackson转换成功, result: {}", result);
                return result;
            } catch (Exception e) {
                logger.warn("Jackson转换失败: {}", e.getMessage());
            }
            
            // 3. 如果上述方法都失败，尝试使用反射创建对象并设置属性
            JSONObject jsonObj = JSON.parseObject(content);
            Object instance = valueType.getDeclaredConstructor().newInstance();
            
            for (String key : jsonObj.keySet()) {
                try {
                    // 获取属性的setter方法
                    String setterName = "set" + key.substring(0, 1).toUpperCase() + key.substring(1);
                    Object value = jsonObj.get(key);
                    
                    // 查找对应的setter方法
                    for (Method method : valueType.getMethods()) {
                        if (method.getName().equals(setterName) && method.getParameterCount() == 1) {
                            Class<?> paramType = method.getParameterTypes()[0];
                            // 转换值类型
                            Object convertedValue = convertValueToTargetType(value, paramType);
                            method.invoke(instance, convertedValue);
                            break;
                        }
                    }
                } catch (Exception e) {
                    logger.warn("设置属性{}失败: {}", key, e.getMessage());
                }
            }
            
            logger.info("反射方式转换成功, result: {}", instance);
            return (T) instance;
            
        } catch (Exception e) {
            logger.error("反序列化失败, error: {}", e.getMessage(), e);
            throw new ServiceException("DESERIALIZE_ERROR", "反序列化异常: " + e.getMessage(), e);
        }
    }
    
    /**
     * 将值转换为目标类型
     */
    private static Object convertValueToTargetType(Object value, Class<?> targetType) {
        if (value == null) {
            return null;
        }
        
        try {
            // 处理基本类型
            if (targetType == String.class) {
                return value.toString();
            } else if (targetType == Integer.class || targetType == int.class) {
                return Integer.valueOf(value.toString());
            } else if (targetType == Long.class || targetType == long.class) {
                return Long.valueOf(value.toString());
            } else if (targetType == Double.class || targetType == double.class) {
                return Double.valueOf(value.toString());
            } else if (targetType == Boolean.class || targetType == boolean.class) {
                return Boolean.valueOf(value.toString());
            } else if (targetType == Float.class || targetType == float.class) {
                return Float.valueOf(value.toString());
            }
            // 处理复杂类型
            else if (value instanceof JSONObject) {
                return readValueNormal(((JSONObject) value).toJSONString(), targetType);
            }
            // 如果是集合类型
            else if (Collection.class.isAssignableFrom(targetType)) {
                // 这里需要处理集合类型的转换
                return value;
            }
        } catch (Exception e) {
            logger.warn("值转换失败，使用原值: {}", e.getMessage());
        }
        
        return value;
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