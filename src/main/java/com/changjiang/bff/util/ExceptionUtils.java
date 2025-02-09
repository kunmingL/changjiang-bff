package com.changjiang.bff.util;

import java.io.PrintWriter;
import java.io.StringWriter;

/**
 * 异常工具类
 * 主要职责：
 * 1. 提供异常处理的工具方法
 * 2. 支持异常堆栈的格式化
 * 3. 辅助异常信息的提取
 */
public class ExceptionUtils {
    
    /**
     * 获取异常堆栈信息
     */
    public static String getStackTrace(Throwable throwable) {
        if (throwable == null) {
            return "";
        }
        
        StringWriter sw = new StringWriter();
        PrintWriter pw = new PrintWriter(sw);
        throwable.printStackTrace(pw);
        return sw.toString();
    }
    
    /**
     * 获取根异常
     */
    public static Throwable getRootCause(Throwable throwable) {
        if (throwable == null) {
            return null;
        }
        
        Throwable cause = throwable.getCause();
        if (cause == null) {
            return throwable;
        }
        
        return getRootCause(cause);
    }
} 