package com.changjing.bff.service.impl;

import com.alibaba.fastjson2.JSONObject;
import com.changjiang.bff.entity.ServiceInfo;
import com.changjiang.grpc.lib.GrpcServiceGrpc;
import com.changjing.bff.constants.BasicConstants;
import com.changjing.bff.core.ServiceApiInfo;
import com.changjing.bff.core.ServiceApiScanner;
import com.changjing.bff.service.TransferService;

import com.fasterxml.jackson.databind.JavaType;
import io.micrometer.common.util.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

@Service
public class TransferServiceImpl implements TransferService {
    
    private final Logger logger = Logger.getLogger(TransferServiceImpl.class.getName());

    @Override
    public Object executeTransferToCrpcService(Object param, String uri) throws Exception {
        Map<String, Object> resObj = new HashMap<>();
        String paramSeralizeStr = "";
        try {
            ServiceInfo serviceInfo = getCrpcTransServiceInfo(uri);
            if (serviceInfo == null) {
                return executeTransfer(param, uri);
            }
            Class reqType = null;
            Class parameterType = null;
            RequestObject requestObject = buildRequestObjectByInfo(serviceInfo);
            if (requestObject.getAgrsType() != null && requestObject.getAgrsType().length > 0) {
                reqType = requestObject.getAgrsType()[0];
            }
            if (requestObject.getParameterizedTypes() != null && requestObject.getParameterizedTypes().length > 0) {
                parameterType = requestObject.getParameterizedTypes()[0];
            }
            Object reqObjInJson = null;
            Object reqObject = null;
            JSONObject reqObj = ((JSONObject) param);
            Object[] reqParams = null;
            if (reqType != null) {
                if (reqObj == null || reqObj.size() == 0) {
                    reqParams = null;
                } else if (isPrimitiveClass(reqType)) {
                    if (reqObj != null && reqObj.keySet() != null) {
                        Iterator iterator = reqObj.keySet().iterator();
                        while (iterator.hasNext()) {
                            String key = (String) iterator.next();
                            reqObjInJson = reqObj.getObject(key, reqType);
                            break;
                        }
                    }
                    reqObject = reqObjInJson;
                }
            }

            if (reqObject == List.class) {
                if (parameterType != null && reqObject != null && !CollectionUtils.isEmpty((List) reqObject)) {
                    JavaType javaType = NpcsSerializerUtil.getTypeByClass(List.class, parameterType);
                    paramSeralizeStr = NpcsSerializerUtil.writeValueAsStringNormal(reqObject); // 填充后续要打的日志
                    reqObject = NpcsSerializerUtil.readValueNormal(paramSeralizeStr, javaType);
                    reqObject = NpcsSerializerUtil.writeValueAsStringNormal(reqObject, javaType);
                } else {
                    reqObjInJson = NpcsSerializerUtil.writeValueAsStringNormal(reqObj);
                    logger.info("executeTransferToCrpcService, reqType:{}", reqType.getName());
                    if (reqType.getName().contains("PageParm")) {
                        Object req = NpcsSerializerUtil.readValueNormal(String reqObjInJson, reqType);
                        JSONObject dataObj = reqObj.getJSONObject("data");
                        if (dataObj != null) {
                            if (serviceInfo.getSpecClassReferMap() != null && !serviceInfo.getSpecClassReferMap().isEmpty()) {
                                String fieldKey = serviceInfo.getReferField();
                                String referValue = dataObj.getString(fieldKey);
                                if (!StringUtils.isEmpty(referValue)) {
                                    reqType = serviceInfo.getSpecClassReferMap().get(referValue);
                                }
                            }
                            JavaType javaType = NpcsSerializerUtil.getTypeByClass(reqType);
                            reqObject = NpcsSerializerUtil.readValueNormal((String) reqObjInJson, javaType);
                        }
                    }
                    reqParams = new Object[]{reqObject};
                    //2023-01-12加入入参日志
                    logger.info("requestObject.setReqObj reqObj :{}", paramSeralizeStr);
                    requestObject.setReqObj(reqParams);
                    ResponseObject<Object> res = methodInvocationService.executeMethodInvocation(requestObject);
                    //LOGGER.info("TransferServiceImpl.executeTransferToCrpcService, uri:{}, response:{}", uri, NpcsSerializerUtil.writeValueAsStringAccuracy(res));
                    resObj.put(BasicConstants.RES_CODE_KEY, res.getRetCode());
                    resObj.put(BasicConstants.RES_ERR_MSG_KEY, res.getRetMsg());
                    if (res.getResData() instanceof List) {
                        resObj.put(BasicConstants.RES_DATA_LIST_KEY, res.getResData());
                    } else if (res.getResData() != null && isPrimitiveClass(res.getResData().getClass())) {
                        resObj.put(BasicConstants.RES_SIMPLE_DATA_KEY, res.getResData());
                    } else {
                        resObj.put(BasicConstants.RES_DATA_KEY, res.getResData());
                    }
                }
            } else {
                reqObjInJson = NpcsSerializerUtil.writeValueAsStringNormal(reqObj);
                logger.info("executeTransferToCrpcService, reqType:{}", reqType.getName());
                if (reqType.getName().contains("PageParm")) {
                    Object req = NpcsSerializerUtil.readValueNormal(String reqObjInJson, reqType);
                    JSONObject dataObj = reqObj.getJSONObject("data");
                    if (dataObj != null) {
                        if (serviceInfo.getSpecClassReferMap() != null && !serviceInfo.getSpecClassReferMap().isEmpty()) {
                            String fieldKey = serviceInfo.getReferField();
                            String referValue = dataObj.getString(fieldKey);
                            if (!StringUtils.isEmpty(referValue)) {
                                reqType = serviceInfo.getSpecClassReferMap().get(referValue);
                            }
                        }
                        JavaType javaType = NpcsSerializerUtil.getTypeByClass(reqType);
                        reqObject = NpcsSerializerUtil.readValueNormal((String) reqObjInJson, javaType);
                    }
                }
                reqParams = new Object[]{reqObject};
                //2023-01-12加入入参日志
                LOGGER.info("requestObject.setReqObj reqObj :{}", paramSeralizeStr);
                requestObject.setReqObj(reqParams);
                ResponseObject<Object> res = methodInvocationService.executeMethodInvocation(requestObject);
                //LOGGER.info("TransferServiceImpl.executeTransferToCrpcService, uri:{}, response:{}", uri, NpcsSerializerUtil.writeValueAsStringAccuracy(res));
                resObj.put(BasicConstants.RES_CODE_KEY, res.getRetCode());
                resObj.put(BasicConstants.RES_ERR_MSG_KEY, res.getRetMsg());
                if (res.getResData() instanceof List) {
                    resObj.put(BasicConstants.RES_DATA_LIST_KEY, res.getResData());
                } else if (res.getResData() != null && isPrimitiveClass(res.getResData().getClass())) {
                    resObj.put(BasicConstants.RES_SIMPLE_DATA_KEY, res.getResData());
                } else {
                    resObj.put(BasicConstants.RES_DATA_KEY, res.getResData());
                }
            }
        } catch (NpcsGTWException ne) {
            ne.printStackTrace();
            resObj.put(BasicConstants.RES_CODE_KEY, ne.getExceptionCode());
            resObj.put(BasicConstants.RES_ERR_MSG_KEY, ne.getMessage());
            LOGGER.error("TransferService.executeTransferToCrpcService,reqParams:{}", uri.concat(",").concat(paramSeralizeStr), ne);
        } catch (Exception e) {
            e.printStackTrace();
            resObj.put(BasicConstants.RES_CODE_KEY, PubConstants.ERROR_INVOKE_EXCEPTION);
            resObj.put(BasicConstants.RES_ERR_MSG_KEY, e.getMessage());
            LOGGER.error("TransferService.executeTransferToCrpcService,reqParams:{}", uri.concat(",").concat(paramSeralizeStr), e);
        }
        return resObj;
    }

} 