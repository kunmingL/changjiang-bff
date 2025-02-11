package com.changjiang.bff.web;

import com.alibaba.fastjson2.JSONObject;
import com.changjiang.bff.constants.BasicConstants;
import com.changjiang.bff.object.response.Result;
import com.changjiang.elearn.api.service.SpokenEnglish;
import com.changjiang.grpc.annotation.GrpcReference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

@RestController
@RequestMapping(value = "/changjiang")
public class GenerateController extends DefaultController {

    private final Logger logger = LoggerFactory.getLogger(GenerateController.class);

    @GrpcReference(register = "elearn")
    private SpokenEnglish spokenEnglish;
    /**
     * 处理所有POST请求
     * @param inputObject
     * @return
     * @param <T>
     */

    // 允许跨域请求，接受所有来源的请求
    @CrossOrigin(origins = "*")
    // 处理所有以POST方法发送的请求，返回JSON格式的数据
    @RequestMapping(value = "/**", produces = MediaType.APPLICATION_JSON_VALUE, method = RequestMethod.POST)
    public Result<?> executeLogic(@RequestBody JSONObject inputObject) {
//        String kunming = spokenEnglish.spokenEnglish("kunming");
//        return kunming;

        // 初始化响应码、数据和错误信息
        String code = null;
        Object resData = null;
        String errMsg = null;
        String uri = null;

        // 打印特定密钥（已注释掉）
        //System.out.println(NpcsDataMaskUtil.CREATE_KEY);

        try {
            // 获取当前请求的属性
            ServletRequestAttributes attrs = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
            // 如果请求属性为空，则返回错误信息
            if (attrs == null) {
                return buildResponseBody(resData,BasicConstants.D1RPG01, "请求信息错误");
            }
            // 获取请求的URI并去除空格
            uri = attrs.getRequest().getRequestURI().trim();
            // 调用方法处理请求并返回结果
            @SuppressWarnings("unchecked")
            Result<?> objectResult = executeTransferToCrpcService(inputObject, attrs.getRequest());
            return objectResult;
        } catch (Exception e) {
            // 打印异常信息并记录日志
            e.printStackTrace();
            logger.error("GenerateController.executeLogic,path:{}, stacktrace:{}", uri, e);
        }

        // 返回构建的响应体
        return buildResponseBody(resData, code, errMsg);
    }
}