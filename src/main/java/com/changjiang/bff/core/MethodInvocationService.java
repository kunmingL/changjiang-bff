package com.changjiang.bff.core;

import com.changjiang.bff.entity.RequestObject;
import com.changjiang.bff.entity.ResponseObject;
import com.changjiang.bff.entity.PageResult;
import com.changjiang.bff.exception.ServiceException;
import com.changjiang.bff.util.NpcsDataMaskUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 方法调用服务类
 * 主要职责：
 * 1. 执行具体的服务方法调用
 * 2. 处理方法调用的参数转换
 * 3. 管理调用结果的数据脱敏
 * 
 * 使用场景：
 * - 服务方法的反射调用
 * - 参数类型转换和验证
 * - 响应数据的脱敏处理
 * 
 * 调用关系：
 * - 被TransferService调用执行方法
 * - 调用NpcsDataMaskUtil处理数据脱敏
 * - 与ServiceApiInfo配合获取方法信息
 */
@Component
public class MethodInvocationService {

   /** 
    * 日志记录器
    * 用于记录方法调用的详细信息
    */
   private static final Logger logger = LoggerFactory.getLogger(MethodInvocationService.class);

   @Autowired
   private CrpcTransferService crpcTransferService;

   public MethodInvocationService() {
   }

   /**
    * 执行方法调用
    * 主要功能：
    * 1. 执行目标方法调用
    * 2. 处理返回结果脱敏
    * 3. 封装统一响应格式
    * 
    * @param requestObject 请求对象
    * @return 响应对象
    */
   public ResponseObject<Object> executeMethodInvocation(RequestObject requestObject) throws ServiceException {
       try {
           // 执行RPC调用
           ResponseObject<Object> responseObject = this.crpcTransferService.methodInvocation(requestObject);
           
           // 处理数据脱敏
           if (requestObject.isDataMask() && responseObject.getResData() != null) {
               if (responseObject.getResData() instanceof Collection) {
                   Object collect = ((Collection) responseObject.getResData())
                           .stream()
                           .map(NpcsDataMaskUtil::doDataMask)
                           .collect(Collectors.toList());
                   responseObject.setResData(collect);
               } else {
                   Object encryptedData = NpcsDataMaskUtil.doDataMask(responseObject.getResData());
                   responseObject.setResData(encryptedData);
               }
           }
           return responseObject;
           
       } catch (Exception e) {
           logger.error("Method invocation error", e);
           throw new ServiceException("SYSTEM_ERROR", "服务调用异常", e);
       }
   }

   /**
    * 执行方法调用前的参数验证
    */
   private void validateRequest(RequestObject requestObject) throws ServiceException {
       if (requestObject == null) {
           throw new ServiceException("INVALID_REQUEST", "请求对象不能为空");
       }
       
       if (StringUtils.isEmpty(requestObject.getUri())) {
           throw new ServiceException("INVALID_URI", "服务URI不能为空");
       }
       
       if (requestObject.getReqObj() == null) {
           throw new ServiceException("INVALID_PARAMS", "请求参数不能为空");
       }
   }
   
   /**
    * 处理响应结果
    */
   private ResponseObject<Object> processResponse(ResponseObject<Object> response, boolean needMask) {
       if (response == null || response.getResData() == null) {
           return response;
       }
       
       try {
           Object data = response.getResData();
           
           // 处理分页结果
           if (data instanceof PageResult) {
               PageResult<?> pageResult = (PageResult<?>) data;
               if (needMask) {
                   // List<?> maskedContent = pageResult.getContent().stream()
                   //     .map(NpcsDataMaskUtil::doDataMask)
                   //     .collect(Collectors.toList());
                  // pageResult.setContent(maskedContent);
               }
               return response;
           }
           
           // 处理集合结果
           if (data instanceof Collection) {
               if (needMask) {
                   List<?> maskedList = ((Collection<?>) data).stream()
                       .map(NpcsDataMaskUtil::doDataMask)
                       .collect(Collectors.toList());
                   response.setResData(maskedList);
               }
               return response;
           }
           
           // 处理普通对象
           if (needMask) {
               Object maskedData = NpcsDataMaskUtil.doDataMask(data);
               response.setResData(maskedData);
           }
           
           return response;
           
       } catch (Exception e) {
           logger.error("Process response error", e);
           throw new ServiceException("RESPONSE_ERROR", "处理响应结果异常", e);
       }
   }
}
