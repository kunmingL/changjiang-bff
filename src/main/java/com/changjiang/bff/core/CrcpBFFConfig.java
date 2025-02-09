package com.changjiang.bff.core;

/**
 * CRPC BFF配置类
 * 主要职责：
 * 1. 存储CRPC服务的配置信息
 * 2. 管理服务调用的基础参数
 * 3. 提供配置信息的访问接口
 * 
 * 使用场景：
 * - 服务地址和协议配置
 * - 超时时间设置
 * - 注册中心配置
 * 
 * 调用关系：
 * - 被NpcsgptwPropertyUtils创建和管理
 * - 被CrpcReferenceConfigCacheLoader使用
 * - 被CrpcTransferService用于服务调用
 */
public class CrcpBFFConfig {
    /** 
     * 服务地址
     * 用于指定服务的网络位置
     * 被服务调用时用于建立连接
     */
    private String address;
    
    /** 
     * 协议类型
     * 用于指定服务调用的协议
     * 支持多种RPC协议
     */
    private String protocol;
    
    /** 
     * 超时时间
     * 服务调用的超时限制
     * 单位：毫秒
     */
    private int timeoutUtils;
    
    /** 
     * 注册ID
     * 服务在注册中心的唯一标识
     * 用于服务发现和路由
     */
    private String registryId;

    // Getters and Setters
    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    public String getProtocol() {
        return protocol;
    }

    public void setProtocol(String protocol) {
        this.protocol = protocol;
    }

    public int getTimeoutUtils() {
        return timeoutUtils;
    }

    public void setTimeoutUtils(int timeoutUtils) {
        this.timeoutUtils = timeoutUtils;
    }

    public String getRegistryId() {
        return registryId;
    }

    public void setRegistryId(String registryId) {
        this.registryId = registryId;
    }
}
