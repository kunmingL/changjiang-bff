package com.changjiang.bff.object.response;

/**
 * 统一响应结果类
 * 主要职责：
 * 1. 提供统一的HTTP接口响应格式
 * 2. 封装业务数据和状态信息
 * 3. 支持泛型数据类型
 * 
 * 使用场景：
 * - 作为所有HTTP接口的统一响应格式
 * - 在Controller层封装业务处理结果
 * - 向客户端提供标准的数据结构
 * 
 * 调用关系：
 * - 被DefaultController.buildResponseBody方法创建
 * - 被所有继承DefaultController的控制器使用
 * - 最终序列化为JSON返回给客户端
 */
public class Result<T> {

    /** 
     * 响应消息
     * 用于向客户端传递处理结果的描述信息
     * 在处理失败时提供错误说明
     */
    private String msg;
    
    /** 
     * 响应代码
     * 用于标识处理结果的状态
     * 遵循预定义的状态码规范
     */
    private String code;
    
    /** 
     * 响应数据
     * 用于存储实际的业务数据
     * 支持泛型，可以适应不同的数据类型
     */
    private T data;

    /**
     * 获取响应消息
     * @return 当前的响应消息
     */
    public String getMsg() {
        return msg;
    }

    /**
     * 设置响应消息
     * @param msg 要设置的响应消息
     */
    public void setMsg(String msg) {
        this.msg = msg;
    }

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public T getData() {
        return data;
    }

    public void setData(T data) {
        this.data = data;
    }
}
