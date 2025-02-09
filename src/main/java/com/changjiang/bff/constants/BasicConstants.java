package com.changjiang.bff.constants;

/**
 * 基础常量类
 * 主要职责：
 * 1. 定义系统基础常量
 * 2. 提供统一的常量管理
 * 3. 支持系统配置的统一维护
 * 
 * 使用场景：
 * - 系统配置参数
 * - 错误码定义
 * - 通用状态标识
 * 
 * 调用关系：
 * - 被所有业务类使用
 * - 被配置系统加载
 * - 被日志系统引用
 */
public class BasicConstants {
    /** 
     * 成功状态码
     * 表示操作执行成功
     * 被响应处理用于状态判断
     */
    public static final String SUCCESS_CODE = "200";
    
    /** 
     * 失败状态码
     * 表示操作执行失败
     * 被异常处理用于错误响应
     */
    public static final String ERROR_CODE = "500";
    
    /** 
     * 系统编码
     * 用于标识当前系统
     * 被日志系统用于系统识别
     */
    public static final String SYSTEM_CODE = "BFF";
    
    public static final String RES_CODE_KEY = "code";
    public static final String RES_DATA_KEY = "data";
    public static final String RES_DATA_LIST_KEY = "list";
    public static final String RES_SIMPLE_DATA_KEY = "simpleData";
    public static final String RES_ERR_MSG_KEY = "msg";
    
    public static final String TRADE_SUCCESS = "000000";
    public static final String TRADE_FAILURE_PARAMS_ERROR = "100001";

    public static final String D1RPG01 = "code";
} 