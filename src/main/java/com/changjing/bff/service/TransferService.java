package com.changjing.bff.service;

import com.alibaba.fastjson2.JSONObject;
import com.changjing.bff.core.ServiceApiInfo;

public interface TransferService<T, P> {

    /**
     * 原始方法调用
     * @param param 参数
     * @param uri URI
     * @return 返回结果
     * @throws Exception 异常
     */
    <P extends Object,T extends Object> T executeTransfer(P param, String uri) throws Exception;

    /**
     * 直接调crpc方法
     * @param param 参数
     * @param uri URI
     * @return 返回结果
     * @throws Exception 异常
     */
    <P extends Object, T extends Object> T executeTransferToCrpcService(P param, String uri) throws Exception;

    /**
     * 构建请求实体
     * @param serviceInfo 服务信息
     * @return 请求对象
     */
    RequestObject buildRequestObjectByInfo(ServiceInfo serviceInfo);
}