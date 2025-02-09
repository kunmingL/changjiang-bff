package com.changjiang.bff.exception;

import com.changjiang.bff.entity.ResponseObject;

import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseBody;

/**
 * 全局异常处理器
 * 主要职责：
 * 1. 统一处理系统异常
 * 2. 规范化异常响应格式
 * 3. 提供友好的错误提示
 */
@ControllerAdvice
@Slf4j
public class GlobalExceptionHandler {
    
    /**
     * 处理业务异常
     */
    @ExceptionHandler(ServiceException.class)
    @ResponseBody
    public ResponseObject<Void> handleServiceException(ServiceException e) {
        log.error("Service error: ", e);
        return ResponseObject.error(e.getCode(), e.getMessage());
    }
    
    /**
     * 处理参数验证异常
     */
    @ExceptionHandler(IllegalArgumentException.class)
    @ResponseBody
    public ResponseObject<Void> handleValidationException(IllegalArgumentException e) {
        log.error("Validation error: ", e);
        return ResponseObject.error("400", e.getMessage());
    }
    
    /**
     * 处理系统异常
     */
    @ExceptionHandler(Exception.class)
    @ResponseBody
    public ResponseObject<Void> handleException(Exception e) {
        log.error("System error: ", e);
        return ResponseObject.error("500", "System internal error");
    }
} 