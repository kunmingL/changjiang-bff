package com.changjiang.bff.constants;

/**
 * 公共常量类
 * 主要职责：
 * 1. 定义系统公共使用的常量
 * 2. 提供业务相关的常量配置
 * 3. 管理系统级别的配置项
 * 
 * 使用场景：
 * - 业务逻辑处理
 * - 系统配置管理
 * - 公共参数定义
 * 
 * 调用关系：
 * - 被业务处理类使用
 * - 被配置管理系统使用
 * - 被参数验证系统使用
 */
public class PubConstants {
    /** 
     * 默认编码
     * 系统默认的字符编码
     * 用于字符串处理和文件操作
     */
    public static final String DEFAULT_ENCODING = "UTF-8";
    
    /** 
     * 会话超时时间
     * 用户会话的有效期限
     * 单位：毫秒
     */
    public static final long SESSION_TIMEOUT = 30 * 60 * 1000;
    
    /** 
     * 最大重试次数
     * 服务调用失败时的重试次数
     * 用于提高系统可用性
     */
    public static final int MAX_RETRY_TIMES = 3;
    
    public static final String ERROR_INVOKE_EXCEPTION = "999999";
} 