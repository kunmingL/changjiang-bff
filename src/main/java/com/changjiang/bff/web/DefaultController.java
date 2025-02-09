package com.changjiang.bff.web;

import com.alibaba.fastjson2.JSONObject;
import com.changjiang.bff.dto.LoginUserInfo;
import com.changjiang.bff.object.response.Result;
import com.changjiang.bff.service.TransferService;
import com.changjiang.bff.constants.BasicConstants;
import com.changjiang.bff.constants.PubConstants;
import com.changjiang.bff.dto.SessionInfo;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.Map;
import java.util.logging.Logger;

/**
 * 控制器基类
 * 主要职责：
 * 1. 提供统一的请求处理和响应封装
 * 2. 处理会话信息和用户认证
 * 3. 管理服务调用的生命周期
 * 
 * 继承关系：
 * - 被具体的业务Controller继承，提供通用功能
 * - 调用TransferService处理具体的服务调用
 */
public abstract class DefaultController {

    /** 
     * 日志记录器
     * 用于记录控制器层的请求处理信息和异常
     */
    private final Logger logger = Logger.getLogger(DefaultController.class.getName());
    
    /** 
     * 传输服务
     * 用于处理跨服务调用
     * 被executeTransferToCrpcService方法调用
     */
    @Autowired
    protected TransferService transferService;

    /**
     * 组装会话信息
     * 主要功能：
     * 1. 将用户登录信息转换为会话信息
     * 2. 用于跨请求传递用户上下文
     * 
     * 调用链：
     * Controller.handleRequest 
     * -> DefaultController.executeTransferToCrpcService 
     * -> assembleSessionInfo
     * 
     * @param user 登录用户信息
     * @return 会话信息对象
     */
    protected SessionInfo assembleSessionInfo(LoginUserInfo user) {
        SessionInfo sessionInfo = new SessionInfo();
        sessionInfo.setUserId(user.getUserId());
        sessionInfo.setUserName(user.getUserName());
        sessionInfo.setUserType(user.getUserType());
        sessionInfo.setUserRole(user.getUserRole());
        sessionInfo.setUserOrgId(user.getUserOrgId());
        sessionInfo.setUserOrgName(user.getUserOrgName());
        sessionInfo.setUserOrgType(user.getUserOrgType());
        return sessionInfo;
    }

    /**
     * 构建统一响应体
     * 
     * @param data 响应数据
     * @param code 响应代码
     * @param msg 响应消息
     * @return 封装的响应结果
     */
    protected <T> Result<T> buildResponseBody(T data, String code, String msg) {
        Result<T> result = new Result<>();
        result.setCode(code);
        result.setMsg(msg);
        result.setData(data);
        return result;
    }

    /**
     * 执行CRPC服务调用
     * 处理HTTP请求到CRPC服务的转发调用
     * 
     * @param inputObject 输入参数
     * @param servletRequest HTTP请求对象
     * @return 统一响应结果
     * @throws Exception 调用异常
     */
    protected <T> Result<T> executeTransferToCrpcService(JSONObject inputObject, HttpServletRequest servletRequest) throws Exception {
        // 初始化响应代码、数据和错误消息
        String code = null;
        T resData = null;
        String errMsg = null;
        String uri = null;

        try {
            // 获取请求的URI，用于日志记录和异常处理
            uri = servletRequest.getRequestURI().trim();
            // 登录用户信息（已注释）
            //LoginUserInfo user = SessionUtils.getLoginUserInfo();
            // 记录用户信息日志
            logger.info("controller.executeTransferToCrpcServce.userInfo: " + inputObject);
            // 组装会话信息（已注释）
            //SessionInfo sessionInfo = assembleSessionInfo(user);
            // 设置请求的IP地址（已注释）
            //sessionInfo.setRequestIpAddress(IpUtils.getIpAddress(servletRequest));
            // 将会话信息添加到RRpc上下文中（已注释）
            //RrpcContext.getRrpcContext().getExtensionAreaStr("session_operator", JSON.toJSONString(sessionInfo));
            // 执行跨服务调用到Crpc服务
            T srvRes = (T) transferService.executeTransferToCrpcService(inputObject, uri);

            // 根据服务调用结果进行处理
            if (srvRes instanceof Map) {
                // 如果返回结果是Map类型，提取相应的代码、数据和错误消息
                code = (String) ((Map) srvRes).get(BasicConstants.RES_CODE_KEY);
                if (((Map) srvRes).containsKey(BasicConstants.RES_DATA_KEY)) {
                    resData = (T) ((Map) srvRes).get(BasicConstants.RES_DATA_KEY);
                } else if (((Map) srvRes).containsKey(BasicConstants.RES_DATA_LIST_KEY)) {
                    resData = (T) ((Map) srvRes).get(BasicConstants.RES_DATA_LIST_KEY);
                } else {
                    resData = (T) ((Map) srvRes).get(BasicConstants.RES_SIMPLE_DATA_KEY);
                }
                errMsg = (String) ((Map) srvRes).get(BasicConstants.RES_ERR_MSG_KEY);
            } else if (srvRes == null) {
                // 如果返回结果为空，设置相应的错误代码
                code = BasicConstants.TRADE_FAILURE_PARAMS_ERROR;
            } else {
                // 如果返回结果是其他类型，设置成功代码并返回结果
                code = BasicConstants.TRADE_SUCCESS;
                resData = srvRes;
            }
        } catch (Exception e) {
            // 捕获调用过程中的异常，并记录日志
            code = PubConstants.ERROR_INVOKE_EXCEPTION;
            errMsg = e.getLocalizedMessage();
            logger.severe("DefaultController.executeLogic.path:" + uri + ", error:" + e.getMessage());
        }

        // 根据调用结果构建并返回响应体
        return buildResponseBody(resData, code, errMsg);
    }
}
