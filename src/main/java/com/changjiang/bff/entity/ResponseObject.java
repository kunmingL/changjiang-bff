package com.changjiang.bff.entity;

import lombok.Data;

/**
 * 响应对象类
 * 主要职责：
 * 1. 封装响应结果
 * 2. 提供统一的响应格式
 * 3. 支持错误信息处理
 */
@Data
public class ResponseObject<T> {
    /** 
     * 响应码
     * 标识请求处理的结果状态
     */
    private String code;
    
    /** 
     * 响应消息
     * 提供详细的结果说明
     */
    private String message;
    
    /** 
     * 响应数据
     * 存储实际的业务数据
     */
    private T resData;
    
    /** 
     * 错误堆栈
     * 仅在开发环境显示
     */
    private String stack;
    
    /**
     * 创建成功响应
     */
    public static <T> ResponseObject<T> success(T data) {
        ResponseObject<T> response = new ResponseObject<>();
        response.setCode("200");
        response.setMessage("Success");
        response.setResData(data);
        return response;
    }
    
    /**
     * 创建错误响应
     */
    public static <T> ResponseObject<T> error(String code, String message) {
        ResponseObject<T> response = new ResponseObject<>();
        response.setCode(code);
        response.setMessage(message);
        return response;
    }
}
