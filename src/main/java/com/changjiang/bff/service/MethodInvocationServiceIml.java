package com.changjiang.bff.service;

import com.alibaba.fastjson2.JSONObject;
import com.changjiang.bff.core.ApiScanner;
import com.changjiang.bff.core.ServiceApiInfo;
import com.changjiang.bff.entity.RequestObject;
import com.changjiang.bff.entity.ServiceInfo;
import com.changjiang.bff.service.impl.MethodInvocationService;
import com.changjiang.bff.util.NpcsSerializerUtil;

import com.changjiang.grpc.annotation.GrpcReference;
import com.changjiang.grpc.annotation.GrpcService;
import com.fasterxml.jackson.databind.JavaType;
import io.micrometer.common.util.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import java.lang.reflect.Method;
import java.math.BigDecimal;
import java.util.Iterator;
import java.util.List;

/**
 * 方法调用服务
 * 主要职责：
 * 1. 执行服务方法的实际调用
 * 2. 处理方法调用的参数转换
 * 3. 管理调用过程的异常处理
 *
 * 使用场景：
 * - 执行本地Java方法调用
 * - 处理RPC远程方法调用
 * - 统一的方法调用异常处理
 *
 * 调用关系：
 * - 被TransferServiceImpl调用执行具体方法
 * - 调用MethodIntrospector获取方法信息
 * - 与ServiceApiScanner配合完成服务调用
 */
@Service
public class MethodInvocationServiceIml implements MethodInvocationService {

    /**
     * 日志记录器
     * 用于记录方法调用的过程和异常
     */
    private final Logger logger = LoggerFactory.getLogger(MethodInvocationServiceIml.class.getName());

    @Autowired
    private ApiScanner apiScanner;

    /**
     * 根据URL调用对应的服务方法。
     *
     * @param url    请求的URL
     * @param params 请求参数
     * @return 服务方法的返回值
     * @throws Exception 如果调用失败
     */
    @Override
    public Object invokeService(String url, JSONObject params) throws Exception {
        logger.info("尝试调用服务，URL: {}", url);
        // 1. 从 apiRegistry 中获取 ServiceApiInfo 对象
        ServiceApiInfo apiInfo = apiScanner.getApiRegistry().get(url);
        if (apiInfo == null) {
            logger.error("未找到与URL {} 对应的服务", url);
            throw new IllegalArgumentException("未找到与URL " + url + " 对应的服务");
        }

        // 2. 获取缓存的 gRPC 客户端实例和方法
        Object serviceInstance = apiInfo.getInstance();
        Method method = apiInfo.getMethod();

        // 3. 使用反射调用方法
        try {
            logger.info("调用服务方法: {}.{}", method.getDeclaringClass().getName(), method.getName());
            Object[] objects = handleRequestParams(apiInfo, params);
            if (!method.getParameterTypes()[0].isAssignableFrom(objects[0].getClass())) {
                throw new IllegalArgumentException("参数类型不匹配");
            }
            return method.invoke(serviceInstance, objects);
        } catch (Exception e) {
            logger.error("调用服务方法失败: {}.{}", method.getDeclaringClass().getName(), method.getName(), e);
            throw e;
        }
    }

    /**
     * 处理前端请求参数并递归转换为后端所需的参数类型
     */
    public Object[] handleRequestParams(ServiceApiInfo serviceInfo, JSONObject param) {
        logger.info("开始处理请求参数, serviceInfo: {}, param: {}", serviceInfo, param);

        Class<?>[] reqTypes = serviceInfo.getRequestType();
        if (reqTypes == null || reqTypes.length == 0) {
            logger.warn("请求参数类型为空");
            return new Object[]{null};
        }
        
        Class<?> reqType = reqTypes[0];
        if (param == null || param.isEmpty()) {
            logger.info("请求参数为空");
            return new Object[]{null};
        }

        try {
            Object convertedParam = recursiveConvertParam(param, reqType);
            logger.info("参数处理完成, 转换后的参数: {}", convertedParam);
            return new Object[]{convertedParam};
        } catch (Exception e) {
            logger.error("处理请求参数时发生错误", e);
            throw new RuntimeException("参数转换失败: " + e.getMessage(), e);
        }
    }

    /**
     * 递归转换参数
     * @param param 待转换的参数
     * @param targetType 目标类型
     * @return 转换后的对象
     */
    private Object recursiveConvertParam(Object param, Class<?> targetType) {
        if (param == null) {
            return null;
        }

        // 处理基本类型
        if (isPrimitiveOrWrapper(targetType)) {
            return convertToPrimitiveType(param, targetType);
        }

        // 处理List类型
        if (List.class.isAssignableFrom(targetType)) {
            return handleListTypeRecursively(param, targetType);
        }

        // 处理Map类型
        if (param instanceof JSONObject) {
            JSONObject jsonObj = (JSONObject) param;
            
            // 处理分页对象
            if (targetType.getName().contains("PageParam") || targetType.getName().contains("Page")) {
                //return handlePageType(jsonObj, targetType);
            }

            // 处理普通对象
            try {
                Object instance = targetType.getDeclaredConstructor().newInstance();
                // 获取目标类型的所有字段
                java.lang.reflect.Field[] fields = targetType.getDeclaredFields();
                
                for (java.lang.reflect.Field field : fields) {
                    field.setAccessible(true);
                    String fieldName = field.getName();
                    
                    if (jsonObj.containsKey(fieldName)) {
                        Object fieldValue = jsonObj.get(fieldName);
                        // 递归处理字段值
                        Object convertedValue = recursiveConvertParam(fieldValue, field.getType());
                        field.set(instance, convertedValue);
                    }
                }
                return instance;
            } catch (Exception e) {
                logger.error("递归转换对象失败", e);
                throw new RuntimeException("递归转换对象失败: " + e.getMessage(), e);
            }
        }

        return param;
    }

    /**
     * 获取List的泛型类型
     */
    private Class<?> getGenericType(Class<?> targetType) {
        try {
            // 如果是List类型，直接从方法参数中获取泛型类型
            if (List.class.isAssignableFrom(targetType)) {
                return getListGenericType(targetType);
            }
            
            // 获取字段的所有泛型参数类型
            java.lang.reflect.Field[] fields = targetType.getDeclaredFields();
            for (java.lang.reflect.Field field : fields) {
                field.setAccessible(true);
                if (List.class.isAssignableFrom(field.getType())) {
                    java.lang.reflect.Type genericType = field.getGenericType();
                    if (genericType instanceof java.lang.reflect.ParameterizedType) {
                        java.lang.reflect.ParameterizedType paramType = (java.lang.reflect.ParameterizedType) genericType;
                        java.lang.reflect.Type[] typeArguments = paramType.getActualTypeArguments();
                        if (typeArguments.length > 0) {
                            java.lang.reflect.Type actualType = typeArguments[0];
                            if (actualType instanceof Class) {
                                return (Class<?>) actualType;
                            } else if (actualType instanceof java.lang.reflect.ParameterizedType) {
                                return (Class<?>) ((java.lang.reflect.ParameterizedType) actualType).getRawType();
                            }
                        }
                    }
                }
            }
        } catch (Exception e) {
            logger.warn("获取List泛型类型失败", e);
        }
        return null;
    }

    /**
     * 获取List的具体泛型类型
     */
    private Class<?> getListGenericType(Class<?> targetType) {
        try {
            // 获取字段类型的泛型参数
            java.lang.reflect.Type[] genericInterfaces = targetType.getGenericInterfaces();
            for (java.lang.reflect.Type genericInterface : genericInterfaces) {
                if (genericInterface instanceof java.lang.reflect.ParameterizedType) {
                    java.lang.reflect.ParameterizedType paramType = (java.lang.reflect.ParameterizedType) genericInterface;
                    java.lang.reflect.Type[] typeArguments = paramType.getActualTypeArguments();
                    if (typeArguments.length > 0) {
                        if (typeArguments[0] instanceof Class) {
                            return (Class<?>) typeArguments[0];
                        }
                    }
                }
            }
        } catch (Exception e) {
            logger.warn("获取List泛型类型失败", e);
        }
        return null;
    }

    /**
     * 递归处理List类型
     */
    private Object handleListTypeRecursively(Object param, Class<?> targetType) {
        if (!(param instanceof List)) {
            return null;
        }

        List<?> sourceList = (List<?>) param;
        List<Object> resultList = new java.util.ArrayList<>();
        
        // 获取List的泛型类型
        Class<?> genericType = getGenericType(targetType);
        if (genericType == null) {
            // 如果无法获取泛型类型，尝试从第一个非空元素推断类型
            for (Object item : sourceList) {
                if (item != null) {
                    // 尝试通过反射获取实际类型
                    try {
                        String className = item.getClass().getName();
                        genericType = Class.forName(className);
                        break;
                    } catch (ClassNotFoundException e) {
                        logger.warn("无法加载类: {}", e.getMessage());
                    }
                }
            }
        }
        
        if (genericType == null) {
            logger.warn("无法确定List的泛型类型，将按原样返回");
            return sourceList;
        }

        // 递归处理List中的每个元素
        for (Object item : sourceList) {
            if (item != null) {
                try {
                    // 将JSON对象转换为目标类型
                    if (item instanceof JSONObject) {
                        Object convertedItem = ((JSONObject) item).toJavaObject(genericType);
                        resultList.add(convertedItem);
                    } else {
                        Object convertedItem = recursiveConvertParam(item, genericType);
                        resultList.add(convertedItem);
                    }
                } catch (Exception e) {
                    logger.error("转换List元素失败: {}", e.getMessage());
                    resultList.add(item);
                }
            } else {
                resultList.add(null);
            }
        }

        return resultList;
    }

    /**
     * 转换为基本类型
     */
    private Object convertToPrimitiveType(Object value, Class<?> targetType) {
        if (value == null) {
            return null;
        }

        try {
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
            } else if (targetType == BigDecimal.class) {
                return new BigDecimal(value.toString());
            }
            // 可以根据需要添加其他基本类型的转换
        } catch (Exception e) {
            logger.error("转换基本类型失败: {} -> {}", value, targetType, e);
            throw new RuntimeException("转换基本类型失败", e);
        }
        return value;
    }

    /**
     * 判断是否为基本类型或其包装类
     */
    private boolean isPrimitiveOrWrapper(Class<?> type) {
        return type.isPrimitive() ||
               type == String.class ||
               type == Integer.class ||
               type == Long.class ||
               type == Double.class ||
               type == Float.class ||
               type == Boolean.class ||
               type == Byte.class ||
               type == Short.class ||
               type == Character.class ||
               type == BigDecimal.class;
    }

    /**
     * 构建请求对象
     * @param serviceInfo 服务信息
     * @return 请求对象
     */
    public RequestObject buildRequestObjectByInfo(ServiceApiInfo serviceInfo) {
        RequestObject requestObject = new RequestObject();
        requestObject.setArgsType(serviceInfo.getRequestType());
        //requestObject.setParameterizedTypes(serviceInfo.);
        return requestObject;
    }
} 