package com.changjing.bff.web;

import com.alibaba.fastjson2.JSONObject;
import com.changjing.bff.dto.LoginUserInfo;
import com.changjing.bff.object.response.Result;
import com.changjing.bff.service.TransferService;
import com.changjing.bff.constants.BasicConstants;
import com.changjing.bff.constants.PubConstants;
import com.changjing.bff.dto.SessionInfo;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.Map;
import java.util.logging.Logger;

public abstract class DefaultController {

    private final Logger logger = Logger.getLogger(DefaultController.class.getName());
    
    @Autowired
    protected TransferService transferService;

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

    protected <T> Result<T> buildResponseBody(T data, String code, String msg) {
        Result<T> result = new Result<>();
        result.setCode(code);
        result.setMsg(msg);
        result.setData(data);
        return result;
    }

    /**
     * 执行跨服务调用到Crpc服务的方法
     *
     * 该方法负责处理从HTTP请求转换到Crpc服务的调用它接收一个JSON对象作为输入，
     * 并根据调用的结果返回一个包含调用结果的Result对象此方法处理正常的Crpc服务调用，
     * 以及调用过程中可能抛出的异常
     *
     * @param inputObject 包含调用参数的JSON对象
     * @param servletRequest HTTP请求对象，用于获取请求URI
     * @return 返回一个Result对象，包含调用结果或错误信息
     * @throws Exception 如果在调用过程中发生异常，则抛出此异常
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
