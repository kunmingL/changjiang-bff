package com.changjiang.bff.entity;

import java.util.Map;

import lombok.Data;
import org.springframework.util.StringUtils;

/**
 * 请求对象实体类
 * 主要职责：
 * 1. 封装服务调用的请求参数
 * 2. 提供请求相关的元数据
 * 3. 支持不同类型的服务调用（直接调用/RPC调用）
 * 
 * 使用场景：
 * - 被TransferService用于构建服务调用请求
 * - 被MethodInvocationService用于执行方法调用
 * - 在服务调用链路中传递请求上下文
 */
@Data
public class RequestObject {
    /** 
     * 目标Bean名称
     * 用于Spring容器中的Bean定位
     * 被MethodInvocationService用于获取目标服务实例
     */
    private String beanName;
    
    /** 
     * 接口类型
     * 表示要调用的服务接口类型
     * 被反射机制用于方法调用
     */
    private Class interfaceName;
    
    /** 方法名称 */
    private String methodName;
    
    /** 接口枚举名称 */
    private String interfaceEnumName;
    
    /** 请求参数数组 */
    private Object[] reqObj;
    
    /** 是否直接调用标志 */
    private boolean directCall = true;
    
    /** 参数类型数组 */
    private Class[] argsType;
    
    /** 泛型参数类型数组 */
    private Class[] parameterizedTypes;
    
    /** 数据脱敏标志 */
    private boolean dataMask = false;
    
    /** 协议类型 */
    private String protocol;
    
    /** 服务地址 */
    private String address;

    /** 
     * 服务URI
     * 用于标识目标服务和方法
     */
    private String uri;
    
    /** 
     * 请求参数
     * 存储方法调用的参数值
     */
    private Map<String, Object> params;
    
    /** 
     * 请求头信息
     * 存储调用链路的上下文信息
     */
    private Map<String, String> headers;

    public RequestObject() {
    }

    public String getBeanName() {
        return beanName;
    }

    public void setBeanName(String beanName) {
        this.beanName = beanName;
    }

    public Class getInterfaceName() {
        return interfaceName;
    }

    public void setInterfaceName(Class interfaceName) {
        this.interfaceName = interfaceName;
    }

    public String getMethodName() {
        return methodName;
    }

    public void setMethodName(String methodName) {
        this.methodName = methodName;
    }

    public String getInterfaceEnumName() {
        return interfaceEnumName;
    }

    public void setInterfaceEnumName(String interfaceEnumName) {
        this.interfaceEnumName = interfaceEnumName;
    }

    public Object[] getReqObj() {
        return reqObj;
    }

    public void setReqObj(Object[] reqObj) {
        this.reqObj = reqObj;
    }

    public boolean isDirectCall() {
        return directCall;
    }

    public void setDirectCall(boolean directCall) {
        this.directCall = directCall;
    }

    public Class[] getArgsType() {
        return argsType;
    }

    public void setArgsType(Class[] argsType) {
        this.argsType = argsType;
    }

    public Class[] getParameterizedTypes() {
        return parameterizedTypes;
    }

    public void setParameterizedTypes(Class[] parameterizedTypes) {
        this.parameterizedTypes = parameterizedTypes;
    }

    public boolean isDataMask() {
        return dataMask;
    }

    public void setDataMask(boolean dataMask) {
        this.dataMask = dataMask;
    }

    public String getProtocol() {
        return protocol;
    }

    public void setProtocol(String protocol) {
        this.protocol = protocol;
    }

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    /**
     * 验证请求参数
     * 确保必要参数存在且合法
     */
    public void validate() {
        if (StringUtils.isEmpty(uri)) {
            throw new IllegalArgumentException("URI cannot be empty");
        }
    }
}
