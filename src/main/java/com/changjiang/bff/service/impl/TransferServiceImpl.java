// package com.changjiang.bff.service.impl;
//
// import com.alibaba.fastjson2.JSONObject;
//
// import com.changjiang.bff.entity.ServiceInfo;
// import com.changjiang.grpc.util.NpcsSerializerUtil;
// import com.changjiang.bff.constants.BasicConstants;
// import com.changjiang.bff.core.MethodInvocationService;
// import com.changjiang.bff.core.introspector.MethodIntrospector;
// import com.changjiang.bff.entity.RequestObject;
// import com.changjiang.bff.entity.ResponseObject;
// import com.changjiang.bff.service.TransferService;
//
// import com.fasterxml.jackson.databind.JavaType;
// import lombok.extern.slf4j.Slf4j;
// import org.springframework.beans.factory.annotation.Autowired;
// import org.springframework.stereotype.Service;
// import org.springframework.util.CollectionUtils;
// import org.springframework.util.StringUtils;
//
// import java.lang.reflect.InvocationTargetException;
// import java.lang.reflect.Method;
// import java.math.BigDecimal;
// import java.util.HashMap;
// import java.util.Iterator;
// import java.util.List;
// import java.util.Map;
// import java.util.logging.Logger;
//
// /**
//  * 传输服务实现类
//  * 主要职责：
//  * 1. 处理所有服务间的请求转发和调用
//  * 2. 支持普通Java方法调用和CRPC服务调用
//  * 3. 处理请求参数的转换和响应结果的封装
//  *
//  * 调用关系：
//  * - 被DefaultController调用，用于处理HTTP请求到后端服务的转发
//  * - 调用MethodIntrospector获取服务方法信息
//  * - 调用MethodInvocationService执行具体的服务调用
//  */
// @Service
// @Slf4j
// public class TransferServiceImpl implements TransferService {
//
//     /**
//      * 方法内省器
//      * 用于获取服务方法信息和实例配置
//      * 被executeTransfer方法调用，用于获取目标服务的方法和实例
//      */
//     @Autowired
//     private MethodIntrospector methodIntrospector;
//
//     /**
//      * 方法调用服务
//      * 用于执行具体的方法调用，支持多种调用方式
//      * 被executeTransferToCrpcService方法调用，用于执行CRPC服务调用
//      */
//     @Autowired
//     private MethodInvocationService methodInvocationService;
//
//     /**
//      * 执行服务调用转发
//      * 主要功能：
//      * 1. 验证请求URI的有效性
//      * 2. 获取目标服务的方法和实例配置
//      * 3. 通过反射执行方法调用
//      * 4. 处理调用异常并封装响应结果
//      *
//      * 调用链：
//      * DefaultController.executeTransferToCrpcService
//      * -> TransferServiceImpl.executeTransfer
//      * -> MethodIntrospector.getServiceInstanceByMethod
//      *
//      * @param param 调用参数，包含请求的具体数据
//      * @param uri 服务URI，用于定位目标服务
//      * @return 调用结果
//      * @throws Exception 调用过程中的异常
//      */
//     @Override
//     public Object executeTransfer(Object param, String uri) throws Exception {
//         // 验证URI非空
//         if (!StringUtils.hasText(uri)) {
//             //throw new NpcsGTWException("uri为空", PubConstants.ERROR_INVOKE_NULL_URL_EXCEPTION, new Exception("uri为空"));
//         }
//
//         // 获取服务实例配置
//         Map<Method, Object> srvInstanceConfig = methodIntrospector.getServiceInstanceByMethod(uri);
//         if (srvInstanceConfig == null) {
//             //throw new NpcsGTWException("未找到服务对应方法和实例配置", PubConstants.ERROR_INVOKE_NULL_SERVICE_EXCEPTION, new Exception("未找到服务对应方法和实例配置"));
//         }
//
//         // 记录请求日志
//         //logger.info("TransferServiceImpl.executeTransfer, uri:{}", uri, param, JsonUtils.toJson(param, BasicConstants.DEFAULT_CHARSET, false));
//
//         Object resObj = null;
//         try {
//             // 遍历配置执行方法调用
//             for (Method keySet : srvInstanceConfig.keySet()) {
//                 Method method = keySet;
//                 Object srvIns = srvInstanceConfig.get(method);
//                 // 执行方法调用
//                 resObj = method.invoke(srvIns, param);
//                 // 记录响应日志
//                 logger.info("TransferServiceImpl.executeTransfer, uri:{}, resJSON {}", uri, JsonUtils.toJson(resObj, BasicConstants.DEFAULT_CHARSET, false));
//                 break;
//             }
//         } catch (Exception e) {
//             e.printStackTrace();
//             // 处理调用目标异常
//             if (e instanceof InvocationTargetException) {
//                 Throwable throwable = ((InvocationTargetException) e).getTargetException().getCause();
//                 //throw new NpcsGTWException(throwable.getMessage(), "NGR001", throwable);
//             }
//             // 构造错误响应
//             JSONObject res = new JSONObject();
//             res.put(BasicConstants.RES_CODE_KEY, ErrorCodeConstants.ERROR_INVOKE_EXCEPTION);
//             res.put(BasicConstants.RES_MSG_KEY, e.getMessage());
//             return res;
//         }
//         return resObj;
//     }
//
//     /**
//      * 获取CPCP交易服务信息
//      * 主要功能：
//      * 1. 处理URL前缀
//      * 2. 从不同配置映射中查找服务信息
//      *
//      * 调用链：
//      * TransferServiceImpl.executeTransferToCrpcService
//      * -> getCpcpTransaServiceInfo
//      * -> 配置映射查找
//      *
//      * @param url 服务URL
//      * @return 服务配置信息
//      */
//     private ServiceInfo getCpcpTransaServiceInfo(String url) {
//         // 过滤URL前缀
//         url = url.replace("/npcspcp", "");
//
//         // 依次从不同配置映射中查找服务信息
//         ServiceInfo serviceInfo = BasicConstants.CONFIG_TRANS_MAP.get(url);
//         if (serviceInfo == null) {
//             serviceInfo = BasicConstants.FTO_CONFIGS_MAP.get(url);
//         }
//         if (serviceInfo == null) {
//             serviceInfo = BasicConstants.FTO_INITINFO_CONFIGS_MAP.get(url);
//         }
//         return serviceInfo;
//     }
//
//     /**
//      * 执行GRPC服务调用
//      * @param param 调用参数
//      * @param uri 服务URI
//      * @return 调用结果
//      * @throws Exception 调用异常
//      */
//     @Override
//     public Object executeTransferToCrpcService(Object param, String uri) throws Exception {
//         Map<String, Object> resObj = new HashMap<>();
//         String paramSeralizeStr = "";
//         try {
//             // 获取服务信息
//             ServiceInfo serviceInfo = getCrpcTransServiceInfo(uri);
//             if (serviceInfo == null) {
//                 return executeTransfer(param, uri);
//             }
//
//             // 获取请求参数类型信息
//             Class reqType = null;
//             Class parameterType = null;
//             RequestObject requestObject = buildRequestObjectByInfo(serviceInfo);
//             if (requestObject.getArgsType() != null && requestObject.getArgsType().length > 0) {
//                 reqType = requestObject.getArgsType()[0];
//             }
//             if (requestObject.getParameterizedTypes() != null && requestObject.getParameterizedTypes().length > 0) {
//                 parameterType = requestObject.getParameterizedTypes()[0];
//             }
//
//             // 处理请求参数
//             Object reqObjInJson = null;
//             Object reqObject = null;
//             JSONObject reqObj = ((JSONObject) param);
//             Object[] reqParams = null;
//
//             // 根据不同参数类型进行处理
//             if (reqType != null) {
//                 if (reqObj == null || reqObj.size() == 0) {
//                     reqParams = null;
//                 } else if (isPrimitiveClass(reqType)) {
//                     // 处理基本类型参数
//                     if (reqObj != null && reqObj.keySet() != null) {
//                         Iterator iterator = reqObj.keySet().iterator();
//                         while (iterator.hasNext()) {
//                             String key = (String) iterator.next();
//                             reqObjInJson = reqObj.getObject(key, reqType);
//                             break;
//                         }
//                     }
//                     reqObject = reqObjInJson;
//                 }
//             }
//
//             // 处理List类型参数
//             if (reqObject == List.class) {
//                 if (parameterType != null && reqObject != null && !CollectionUtils.isEmpty((List) reqObject)) {
//                     // 处理泛型List参数
//                     JavaType javaType = NpcsSerializerUtil.getTypeByClass(List.class, parameterType);
//                     paramSeralizeStr = NpcsSerializerUtil.writeValueAsStringNormal(reqObject);
//                     reqObject = NpcsSerializerUtil.readValueNormal(paramSeralizeStr, javaType);
//                     reqObject = NpcsSerializerUtil.writeValueAsStringNormal(reqObject, javaType);
//                 }
//             } else {
//                 // 处理其他类型参数
//                 reqObjInJson = NpcsSerializerUtil.writeValueAsStringNormal(reqObj);
//                 logger.info("executeTransferToCrpcService, reqType:{}", reqType.getName());
//
//                 // 处理分页参数
//                 if (reqType.getName().contains("PageParm")) {
//                     Object req = NpcsSerializerUtil.readValueNormal(String reqObjInJson, reqType);
//                     JSONObject dataObj = reqObj.getJSONObject("data");
//                     if (dataObj != null) {
//                         // 处理特殊类型引用
//                         if (serviceInfo.getSpecClassReferMap() != null && !serviceInfo.getSpecClassReferMap().isEmpty()) {
//                             String fieldKey = serviceInfo.getReferField();
//                             String referValue = dataObj.getString(fieldKey);
//                             if (!StringUtils.isEmpty(referValue)) {
//                                 reqType = serviceInfo.getSpecClassReferMap().get(referValue);
//                             }
//                         }
//                         JavaType javaType = NpcsSerializerUtil.getTypeByClass(reqType);
//                         reqObject = NpcsSerializerUtil.readValueNormal((String) reqObjInJson, javaType);
//                     }
//                 }
//             }
//
//             // 设置请求参数并执行调用
//             reqParams = new Object[]{reqObject};
//             logger.info("requestObject.setReqObj reqObj :{}", paramSeralizeStr);
//             requestObject.setReqObj(reqParams);
//             ResponseObject<Object> res = methodInvocationService.executeMethodInvocation(requestObject);
//
//             // 处理响应结果
//             resObj.put(BasicConstants.RES_CODE_KEY, res.getRetCode());
//             resObj.put(BasicConstants.RES_ERR_MSG_KEY, res.getRetMsg());
//             if (res.getResData() instanceof List) {
//                 resObj.put(BasicConstants.RES_DATA_LIST_KEY, res.getResData());
//             } else if (res.getResData() != null && isPrimitiveClass(res.getResData().getClass())) {
//                 resObj.put(BasicConstants.RES_SIMPLE_DATA_KEY, res.getResData());
//             } else {
//                 resObj.put(BasicConstants.RES_DATA_KEY, res.getResData());
//             }
//
//         } catch (NpcsGTWException ne) {
//             // 处理网关异常
//             ne.printStackTrace();
//             resObj.put(BasicConstants.RES_CODE_KEY, ne.getExceptionCode());
//             resObj.put(BasicConstants.RES_ERR_MSG_KEY, ne.getMessage());
//             LOGGER.error("TransferService.executeTransferToCrpcService,reqParams:{}", uri.concat(",").concat(paramSeralizeStr), ne);
//         } catch (Exception e) {
//             // 处理其他异常
//             e.printStackTrace();
//             resObj.put(BasicConstants.RES_CODE_KEY, PubConstants.ERROR_INVOKE_EXCEPTION);
//             resObj.put(BasicConstants.RES_ERR_MSG_KEY, e.getMessage());
//             LOGGER.error("TransferService.executeTransferToCrpcService,reqParams:{}", uri.concat(",").concat(paramSeralizeStr), e);
//         }
//         return resObj;
//     }
//
//     /**
//      * 判断是否为基本类型
//      * @param clazz 类型
//      * @return 是否基本类型
//      */
//     private boolean isPrimitiveClass(Class clazz) {
//         if (clazz == String.class || clazz == Long.class || clazz == Integer.class ||
//             clazz == BigDecimal.class || clazz == Boolean.class || clazz == List.class) {
//             return true;
//         }
//         return false;
//     }
//
//     /**
//      * 构建请求对象
//      * @param serviceInfo 服务信息
//      * @return 请求对象
//      */
//     @Override
//     public RequestObject buildRequestObjectByInfo(ServiceInfo serviceInfo) {
//         RequestObject requestObject = new RequestObject();
//         requestObject.setInterfaceName(serviceInfo.getMethodName());
//         requestObject.setDirectCall(serviceInfo.isDirectCall());
//         requestObject.setDataMask(serviceInfo.isDataMask());
//         requestObject.setArgsType(serviceInfo.getArgsType());
//         requestObject.setParameterizedTypes(serviceInfo.getParameterizedTypes());
//         requestObject.setProtocol(serviceInfo.getProtocol());
//         requestObject.setAddress(serviceInfo.getAddress());
//         return requestObject;
//     }
//
//     /**
//      * 处理请求参数
//      */
//     private Object processRequestParameter(Object reqObj, Class<?> reqType, ServiceInfo serviceInfo) {
//         try {
//             String reqObjInJson;
//             Object reqObject = reqObj;
//
//             // 处理List类型参数
//             if (reqType == List.class) {
//                 if (serviceInfo.getParameterType() != null && reqObj != null && !CollectionUtils.isEmpty((List) reqObj)) {
//                     // 处理泛型List参数
//                     JavaType javaType = NpcsSerializerUtil.getTypeByClass(List.class, serviceInfo.getParameterType());
//                     String paramSerializeStr = NpcsSerializerUtil.writeValueAsStringNormal(reqObj);
//                     reqObject = NpcsSerializerUtil.readValueNormal(paramSerializeStr, javaType);
//                 }
//             } else {
//                 // 处理其他类型参数
//                 reqObjInJson = NpcsSerializerUtil.writeValueAsStringNormal(reqObj);
//
//                 // 处理分页参数
//                 if (reqType.getName().contains("PageParam")) {
//                     reqObject = processPageParameter(reqObjInJson, reqType, serviceInfo);
//                 } else {
//                     // 处理普通参数
//                     JavaType javaType = NpcsSerializerUtil.getTypeByClass(reqType);
//                     reqObject = NpcsSerializerUtil.readValueNormal(reqObjInJson, javaType);
//                 }
//             }
//
//             return reqObject;
//
//         } catch (Exception e) {
//             logger.error("Process request parameter error", e);
//             throw new ServiceException("PARAMETER_ERROR", "参数处理异常", e);
//         }
//     }
//
//     /**
//      * 处理分页参数
//      */
//     private Object processPageParameter(String reqObjInJson, Class<?> reqType, ServiceInfo serviceInfo) {
//         JSONObject reqObj = JSON.parseObject(reqObjInJson);
//         JSONObject dataObj = reqObj.getJSONObject("data");
//
//         if (dataObj != null) {
//             // 处理特殊类型引用
//             if (serviceInfo.getSpecClassReferMap() != null && !serviceInfo.getSpecClassReferMap().isEmpty()) {
//                 String fieldKey = serviceInfo.getReferField();
//                 String referValue = dataObj.getString(fieldKey);
//                 if (!StringUtils.isEmpty(referValue)) {
//                     reqType = serviceInfo.getSpecClassReferMap().get(referValue);
//                 }
//             }
//
//             JavaType javaType = NpcsSerializerUtil.getTypeByClass(reqType);
//             return NpcsSerializerUtil.readValueNormal(reqObjInJson, javaType);
//         }
//
//         return null;
//     }
// }