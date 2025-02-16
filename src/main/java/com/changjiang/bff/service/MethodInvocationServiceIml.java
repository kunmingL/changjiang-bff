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
     * 处理前端请求参数并转换为适合后端微服务接口的参数类型。
     *
     * @param serviceInfo 后端服务信息对象，包含接口参数类型等相关信息。
     * @param param      前端传入的 JSON 对象参数。
     * @return 转换后的请求参数数组，供后端微服务调用使用。
     */
    public Object[] handleRequestParams(ServiceApiInfo serviceInfo, JSONObject param) {
        logger.info("开始处理请求参数, serviceInfo: {}, param: {}", serviceInfo, param);

        // 1. 获取参数类型信息
        Class<?>[] reqTypes = serviceInfo.getRequestType();
        if (reqTypes == null || reqTypes.length == 0) {
            logger.warn("请求参数类型为空");
            return new Object[]{null};
        }
        Class<?> reqType = reqTypes[0];

        // 2. 如果请求参数为空，返回null
        if (param == null || param.isEmpty()) {
            logger.info("请求参数为空");
            return new Object[]{null};
        }

        try {
            // 3. 处理不同类型的参数
            Object convertedParam = null;

            // 3.1 处理基本类型
            if (isPrimitiveOrWrapper(reqType)) {
                convertedParam = handlePrimitiveType(param, reqType);
            }
            // 3.2 处理List类型
            else if (List.class.isAssignableFrom(reqType)) {
                convertedParam = handleListType(param, reqType);
            }
            // 3.3 处理分页类型
            else if (reqType.getName().contains("PageParam") || reqType.getName().contains("Page")) {
                convertedParam = handlePageType(param, reqType);
            }
            // 3.4 处理普通实体类型
            else {
                convertedParam = handleEntityType(param, reqType);
            }

            logger.info("参数处理完成, 转换后的参数: {}", convertedParam);
            return new Object[]{convertedParam};

        } catch (Exception e) {
            logger.error("处理请求参数时发生错误", e);
            throw new RuntimeException("参数转换失败: " + e.getMessage(), e);
        }
    }

    /**
     * 处理基本类型参数
     */
    private Object handlePrimitiveType(JSONObject param, Class<?> targetType) {
        if (param.isEmpty()) {
            return null;
        }

        String firstKey = param.keySet().iterator().next();
        return param.getObject(firstKey, targetType);
    }

    /**
     * 处理List类型参数
     */
    private Object handleListType(JSONObject param, Class<?> reqType) {
        if (reqType == null) {
            logger.warn("List的泛型类型未指定");
            return null;
        }

        String jsonStr = NpcsSerializerUtil.writeValueAsStringNormal(param);
        JavaType listType = NpcsSerializerUtil.getTypeByClass(List.class, reqType);
        return null;
        //return NpcsSerializerUtil.readValueNormal(jsonStr, listType);
    }

    /**
     * 处理分页类型参数
     */
    private Object handlePageType(JSONObject param, Class<?> pageType) {
        // 1. 提取分页基本参数
        int pageNum = param.getIntValue("pageNum", 1);
        int pageSize = param.getIntValue("pageSize", 10);

        // 2. 获取数据部分
        JSONObject dataObj = param.getJSONObject("data");
        if (dataObj == null) {
            logger.warn("分页参数中缺少data字段");
            return null;
        }

        try {
            // 3. 创建分页对象实例
            Object pageInstance = pageType.getDeclaredConstructor().newInstance();

            // 4. 设置分页基本参数
            Method setPageNum = pageType.getMethod("setPageNum", int.class);
            Method setPageSize = pageType.getMethod("setPageSize", int.class);
            setPageNum.invoke(pageInstance, pageNum);
            setPageSize.invoke(pageInstance, pageSize);

            // 5. 设置查询条件
            Method setData = pageType.getMethod("setData", Object.class);
            setData.invoke(pageInstance, dataObj);

            return pageInstance;
        } catch (Exception e) {
            logger.error("创建分页对象失败", e);
            throw new RuntimeException("创建分页对象失败", e);
        }
    }

    /**
     * 处理实体类型参数
     */
    private Object handleEntityType(JSONObject param, Class<?> entityType) {
        String jsonStr = NpcsSerializerUtil.writeValueAsStringNormal(param);
        return NpcsSerializerUtil.readValueNormal(jsonStr, entityType);
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