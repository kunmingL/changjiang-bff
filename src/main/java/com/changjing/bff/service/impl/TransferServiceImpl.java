package com.changjing.bff.service.impl;

import com.alibaba.fastjson.JSONObject;
import com.changjing.bff.core.ServiceApiInfo;
import com.changjing.bff.core.ServiceApiScanner;
import com.changjing.bff.service.TransferService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.logging.Logger;

@Service
public class TransferServiceImpl implements TransferService {
    
    private final Logger logger = Logger.getLogger(TransferServiceImpl.class.getName());
    
    @Autowired
    private ServiceApiScanner apiScanner;

    /**
     * 执行从CRPC服务转移的调用
     *
     * 本方法根据提供的URI，将输入的JSON对象转发到相应的CRPC服务进行处理
     * 它首先通过API扫描器获取指定URI的API信息，如果找到对应的API信息，
     * 则调用executeServiceApi方法来执行该API并返回结果如果未找到对应的API信息，
     * 则抛出运行时异常，指示没有找到对应的服务
     *
     * @param inputObject 输入的JSON对象，包含调用API所需的所有参数
     * @param uri 指定的API URI，用于识别和定位要调用的服务
     * @return Object 返回执行API的结果，具体类型取决于所调用的API
     * @throws Exception 如果执行API时发生任何错误，将抛出异常
     */
    @Override
    public Object executeTransferToCrpcService(JSONObject inputObject, String uri) throws Exception {
        // 根据URI获取API信息
        ServiceApiInfo apiInfo = apiScanner.getApiInfo(uri);
        // 如果API信息存在，则执行该API
        if (apiInfo != null) {
            return executeServiceApi(apiInfo, inputObject);
        }

        // 如果没有找到对应的服务，抛出异常
        throw new RuntimeException("No service found for uri: " + uri);
    }
    
    @Override
    public Object executeServiceApi(ServiceApiInfo apiInfo, JSONObject inputObject) throws Exception {
        try {
            // 直接调用目标方法
            return apiInfo.getMethod().invoke(apiInfo.getInstance(), inputObject);
        } catch (Exception e) {
            logger.severe("Failed to execute service api: " + e.getMessage());
            throw e;
        }
    }
} 