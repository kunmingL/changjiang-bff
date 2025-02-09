package com.changjiang.bff.entity;
import lombok.Data;

/**
 * 服务信息实体类
 * 主要职责：
 * 1. 存储服务的配置信息
 * 2. 提供服务调用所需的元数据
 * 3. 管理服务的生命周期信息
 * 
 * 使用场景：
 * - 服务注册和发现
 * - 服务配置管理
 * - 服务调用参数验证
 * 
 * 调用关系：
 * - 被ServiceApiScanner创建和管理
 * - 被TransferService用于服务调用
 * - 被MethodIntrospector用于方法解析
 */
@Data
public class ServiceInfo {
    /** 
     * 服务ID
     * 服务的唯一标识
     * 用于服务定位和管理
     */
    private String serviceId;
    
    /** 
     * 服务名称
     * 服务的业务名称
     * 用于日志记录和显示
     */
    private String serviceName;
    
    /** 
     * 服务版本
     * 服务的版本号
     * 用于版本控制和兼容性管理
     */
    private String version;
    
    /** 
     * 服务地址
     * 服务的访问地址
     * 用于服务调用和路由
     */
    private String address;
    
    /** 
     * 服务协议
     * 服务的通信协议(如dubbo, mesh等)
     * 用于服务通信方式的选择
     */
    private String protocol;
    
    /** 
     * 服务状态
     * 服务的运行状态
     * 用于服务健康检查
     */
    private String status;
    
    /** 
     * 超时时间(毫秒)
     * 服务调用的超时限制
     */
    private int timeoutMills = 30000;
    
    /** 
     * 接口类
     * 服务的接口定义类
     * 用于服务代理创建和方法调用
     */
    private Class<?> interfaceClass;
    
    /** 
     * 方法名
     * 当前调用的方法名
     * 用于方法定位和调用
     */
    private String methodName;
    
    /** 
     * 重试次数
     * 服务调用失败时的重试次数
     */
    private int retries = 2;
    
    /** 
     * 负载均衡策略
     * 服务调用的负载均衡方式
     */
    private String loadbalance = "random";
    
    /** 
     * 集群策略
     * 服务的集群调用策略
     */
    private String cluster = "failover";
    
    /** 
     * 是否需要数据脱敏
     * 控制响应数据是否需要脱敏处理
     */
    private boolean dataMask = false;
    
    /** 
     * 备份服务配置
     * 用于服务容灾
     */
    private ServiceInfo backupService;
    
    /**
     * 获取服务的唯一标识
     * 用于缓存key的生成
     */
    public String getUniqueKey() {
        return String.format("%s_%s_%s", 
            interfaceClass != null ? interfaceClass.getName() : "",
            methodName,
            version);
    }
} 