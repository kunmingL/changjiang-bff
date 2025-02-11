package com.changjiang.bff.service;

import com.alibaba.fastjson2.JSONObject;
import com.changjiang.bff.core.ApiScanner;
import com.changjiang.bff.core.ServiceApiInfo;
import com.changjiang.bff.entity.RequestObject;
import com.changjiang.bff.entity.ServiceInfo;
import com.changjiang.bff.service.impl.MethodInvocationService;
import com.changjiang.bff.util.NpcsSerializerUtil;
import com.changjiang.elearn.api.service.SpokenEnglish;
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
     * 处理前端请求参数并转换为适合后端微服务接口的参数类型。
     *
     * @param serviceInfo 后端服务信息对象，包含接口参数类型等相关信息。
     * @param param       前端传入的 JSON 对象参数。
     * @return 转换后的请求参数数组，供后端微服务调用使用。
     */
    public Object[] handleRequestParams(ServiceApiInfo serviceInfo, JSONObject param) {
        // 初始化变量
        Class<?> reqType = null; // 请求参数类型
        Class<?> parameterType = null; // 泛型参数类型（如 List 的泛型类型）
        RequestObject requestObject = buildRequestObjectByInfo(serviceInfo); // 根据服务信息构建请求对象

        // 获取请求参数类型信息
        if (requestObject.getArgsType() != null && requestObject.getArgsType().length > 0) {
            reqType = requestObject.getArgsType()[0]; // 获取请求参数的具体类型
        }
        if (requestObject.getParameterizedTypes() != null && requestObject.getParameterizedTypes().length > 0) {
            parameterType = requestObject.getParameterizedTypes()[0]; // 获取泛型参数类型
        }

        // 初始化请求参数相关变量
        Object reqObjInJson = null;
        Object reqObject = null;
        JSONObject reqObj = param;

        // 如果请求参数为空或无数据，则直接返回空数组
        if (reqObj == null || reqObj.isEmpty()) {
            return new Object[]{null};
        }

        // 根据不同参数类型进行处理
        if (reqType != null) {
            if (isPrimitiveClass(reqType)) {
                // 处理基本类型参数（如 int、long、String 等）
                Iterator<String> iterator = reqObj.keySet().iterator();
                while (iterator.hasNext()) {
                    String key = iterator.next();
                    reqObjInJson = reqObj.getObject(key, reqType); // 将 JSON 中的值转换为目标类型
                    break; // 只取第一个字段的值
                }
                reqObject = reqObjInJson;
            } else if (reqType.equals(List.class)) {
                // 处理 List 类型参数
                if (parameterType != null && reqObj != null && !CollectionUtils.isEmpty((List<?>) reqObj)) {
                    // 处理泛型 List 参数
                    JavaType javaType = NpcsSerializerUtil.getTypeByClass(List.class, parameterType);
                    String paramSerializeStr = NpcsSerializerUtil.writeValueAsStringNormal(reqObj); // 序列化为 JSON 字符串
                    reqObject = NpcsSerializerUtil.readValueNormal(paramSerializeStr, javaType); // 反序列化为目标类型
                }
            } else {
                // 处理其他复杂类型参数
                try {
                    reqObjInJson = NpcsSerializerUtil.writeValueAsStringNormal(reqObj); // 将 JSON 对象序列化为字符串
                    logger.info("handleRequestParams, reqType:{}", reqType.getName());

                    // 特殊处理分页参数（PageParm 类型）
                    if (reqType.getName().contains("PageParm")) {
                        //Object pageReq = NpcsSerializerUtil.readValueNormal(reqObjInJson, reqType); // 反序列化为分页对象
                        JSONObject dataObj = reqObj.getJSONObject("data"); // 获取分页数据部分

                        if (dataObj != null) {
//                            // 处理特殊类型引用
//                            if (serviceInfo.getSpecClassReferMap() != null && !serviceInfo.getSpecClassReferMap().isEmpty()) {
//                                String fieldKey = serviceInfo.getReferField(); // 获取引用字段名
//                                String referValue = dataObj.getString(fieldKey); // 获取引用字段值
//
//                                if (!StringUtils.isEmpty(referValue)) {
//                                    reqType = serviceInfo.getSpecClassReferMap().get(referValue); // 根据引用值获取目标类型
//                                }
//                            }
//
//                            // 构建目标类型的 JavaType 并反序列化
//                            JavaType javaType = NpcsSerializerUtil.getTypeByClass(reqType);
//                            reqObject = NpcsSerializerUtil.readValueNormal(reqObjInJson, javaType);
                        }
                    } else {
                        // 默认处理方式：直接反序列化为目标类型
                        //reqObject = NpcsSerializerUtil.readValueNormal(reqObjInJson, reqType);
                    }
                } catch (Exception e) {
                    logger.error("handleRequestParams failed to convert param, error: {}", e.getMessage(), e);
                    throw new RuntimeException("Failed to convert request parameters", e);
                }
            }
        }

        // 设置请求参数并返回
        Object[] reqParams = new Object[]{reqObject};
        logger.info("handleRequestParams, serialized request object: {}", reqObjInJson);
        requestObject.setReqObj(reqParams); // 设置请求对象中的参数
        return reqParams;
    }

         /**
      * 判断是否为基本类型
      * @param clazz 类型
      * @return 是否基本类型
      */
     private boolean isPrimitiveClass(Class clazz) {
         if (clazz == String.class || clazz == Long.class || clazz == Integer.class ||
             clazz == BigDecimal.class || clazz == Boolean.class || clazz == List.class) {
             return true;
         }
         return false;
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