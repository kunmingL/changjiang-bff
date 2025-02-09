package com.changjiang.bff.exception;
/**
 * 服务异常类
 * 主要职责：
 * 1. 统一的服务异常处理
 * 2. 提供异常信息的封装
 * 3. 支持异常链传递
 */
public class ServiceException extends RuntimeException {
    /** 
     * 错误码
     * 异常的唯一标识
     * 用于异常分类和处理
     */
    private String code;
    
    /** 
     * 错误消息
     * 异常的详细描述
     * 用于异常展示和日志记录
     */
    private String message;
    
    /**
     * 构造函数
     * @param code 错误码
     * @param message 错误消息
     */
    public ServiceException(String code, String message) {
        super(message);
        this.code = code;
        this.message = message;
    }
    
    /**
     * 构造函数
     * @param code 错误码
     * @param message 错误消息
     * @param cause 原始异常
     */
    public ServiceException(String code, String message, Throwable cause) {
        super(message, cause);
        this.code = code;
        this.message = message;
    }
    
    public String getCode() {
        return code;
    }
    
    @Override
    public String getMessage() {
        return message;
    }
} 