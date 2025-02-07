package com.changjing.bff.service;

import com.alibaba.fastjson.JSONObject;
import com.changjing.bff.core.ServiceApiInfo;

public interface TransferService {
    Object executeTransferToCrpcService(JSONObject inputObject, String uri) throws Exception;
    Object executeServiceApi(ServiceApiInfo apiInfo, JSONObject inputObject) throws Exception;
} 