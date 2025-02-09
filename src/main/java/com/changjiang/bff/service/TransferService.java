package com.changjiang.bff.service;

import com.changjiang.bff.entity.ServiceInfo;
import com.changjiang.bff.entity.RequestObject;

/**
 * 传输服务接口
 * 负责处理服务间的请求转发和调用，支持普通调用和CRPC服务调用
 * @param <T> 返回结果类型
 * @param <P> 请求参数类型
 */
public interface TransferService<T, P> {

    /**
     * 执行普通服务调用
     * 通过反射方式调用目标服务的方法
     * 
     * @param param 调用参数
     * @param uri 目标服务的URI
     * @return 调用结果
     * @throws Exception 调用过程中的异常
     */
    <P extends Object,T extends Object> T executeTransfer(P param, String uri) throws Exception;

    /**
     * 执行CRPC服务调用
     * 将请求转发到CRPC服务并处理响应
     * 
     * @param param 调用参数
     * @param uri 目标服务的URI
     * @return 调用结果
     * @throws Exception 调用过程中的异常
     */
    <P extends Object, T extends Object> T executeTransferToCrpcService(P param, String uri) throws Exception;

    /**
     * 根据服务信息构建请求对象
     * 
     * @param serviceInfo 服务配置信息
     * @return 封装的请求对象
     */
    RequestObject buildRequestObjectByInfo(ServiceInfo serviceInfo);
}