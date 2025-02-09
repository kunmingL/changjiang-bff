package com.changjiang.bff.core;

import com.changjiang.bff.entity.ServiceInfo;
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

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * CRPC传输服务
 * 主要职责：
 * 1. 执行CRPC服务的方法调用
 * 2. 处理请求参数的序列化
 * 3. 管理服务调用的异常处理
 * 
 * 使用场景：
 * - CRPC服务方法调用
 * - 参数序列化和反序列化
 * - 异常处理和日志记录
 * 
 * 调用关系：
 * - 被MethodInvocationService调用
 * - 调用CrpcReferenceConfigCacheLoader获取代理
 * - 与ServiceApiScanner配合完成服务调用
 */
@Component
public class CrpcTransferService {

    /** 
     * 日志记录器
     * 用于记录服务调用的关键信息
     */
    private static final Logger logger = LoggerFactory.getLogger(CrpcTransferService.class);

    @Autowired
    private CrpcReferenceConfigCacheLoader crpcReferenceConfigCacheLoader;


    public CrpcTransferService() {}

    /**
     * 参数检查和构建服务信息
     */
    private ServiceInfo doParamCheckAndBuild(RequestObject requestObject) throws ServiceException {
        if (requestObject == null) {
            throw new ServiceException("INVALID_REQUEST", "请求对象不能为空");
        }
        
        Class<?> interfaceClass = requestObject.getInterfaceName();
        if (interfaceClass == null) {
            throw new ServiceException("INVALID_INTERFACE", "接口类不能为空");
        }
        
        ServiceInfo serviceInfo = new ServiceInfo();
        serviceInfo.setInterfaceClass(interfaceClass);
        serviceInfo.setMethodName(requestObject.getMethodName());
        serviceInfo.setAddress(requestObject.getAddress());
        serviceInfo.setProtocol(requestObject.getProtocol());
        return serviceInfo;
    }

    /**
     * 执行方法调用
     */
    public ResponseObject<Object> methodInvocation(RequestObject requestObject) {
        ResponseObject<Object> response = new ResponseObject<>();
        
        try {
            // 参数校验和服务信息构建
            ServiceInfo serviceInfo = doParamCheckAndBuild(requestObject);
            
            // 获取服务代理
            Object serviceProxy = crpcReferenceConfigCacheLoader.getRpcProxy(serviceInfo);
            
            // 执行方法调用
            Object result = invokeMethod(serviceProxy, serviceInfo.getMethodName(), requestObject.getReqObj());
            
            // 处理返回结果
            Object processedResult = processRpcResponse(result);
            response.setCode("200");
            response.setResData(processedResult);
            
            return response;
            
        } catch (Exception e) {
            logger.error("Method invocation error", e);
            handleException(e, response);
            return response;
        }
    }

    /**
     * 执行方法调用
     */
    private Object invokeMethod(Object target, String methodName, Object[] args) throws Exception {
        Method method = findMethod(target.getClass(), methodName, args);
        if (method == null) {
            throw new ServiceException("METHOD_NOT_FOUND", "方法不存在: " + methodName);
        }
        method.setAccessible(true);
        return method.invoke(target, args);
    }

    /**
     * 查找匹配的方法
     */
    private Method findMethod(Class<?> targetClass, String methodName, Object[] args) {
        Method[] methods = targetClass.getMethods();
        for (Method method : methods) {
            if (method.getName().equals(methodName) && isParameterMatched(method, args)) {
                return method;
            }
        }
        return null;
    }

    /**
     * 检查参数是否匹配
     */
    private boolean isParameterMatched(Method method, Object[] args) {
        if (args == null) {
            return method.getParameterCount() == 0;
        }
        if (method.getParameterCount() != args.length) {
            return false;
        }
        Class<?>[] paramTypes = method.getParameterTypes();
        for (int i = 0; i < args.length; i++) {
            if (args[i] != null && !paramTypes[i].isAssignableFrom(args[i].getClass())) {
                return false;
            }
        }
        return true;
    }

    /**
     * 处理RPC响应结果
     */
    private Object processRpcResponse(Object resObj) {
        if (resObj == null) {
            return null;
        }
        
        try {
            // 处理分页结果
            if (isPageResult(resObj)) {
                return processPageResult(resObj);
            }
            
            // 处理集合结果
            if (resObj instanceof Collection) {
                return ((Collection<?>) resObj).stream()
                    .map(this::processDataMask)
                    .collect(Collectors.toList());
            }
            
            // 处理普通对象
            return processDataMask(resObj);
            
        } catch (Exception e) {
            logger.error("Process response error", e);
            return resObj;
        }
    }

    /**
     * 判断是否为分页结果
     */
    private boolean isPageResult(Object obj) {
        return obj != null && 
               obj.getClass().getName().endsWith("Page") &&
               !obj.getClass().getName().endsWith("PageImpl");
    }

    /**
     * 处理分页结果
     */
    private PageResult<?> processPageResult(Object pageResult) {
        try {
            Method getContent = pageResult.getClass().getMethod("getContent");
            Method getTotal = pageResult.getClass().getMethod("getTotal");
            Method getPageNum = pageResult.getClass().getMethod("getPageNum");
            Method getPageSize = pageResult.getClass().getMethod("getPageSize");

            List<?> content = (List<?>) getContent.invoke(pageResult);
            long total = (long) getTotal.invoke(pageResult);
            int pageNum = (int) getPageNum.invoke(pageResult);
            int pageSize = (int) getPageSize.invoke(pageResult);

            // 处理内容脱敏
            List<?> maskedContent = content.stream()
                .map(this::processDataMask)
                .collect(Collectors.toList());

            return PageResult.of(maskedContent, total, pageNum, pageSize);
        } catch (Exception e) {
            logger.error("Process page result error", e);
            return null;
        }
    }

    /**
     * 处理数据脱敏
     */
    private Object processDataMask(Object obj) {
        if (obj == null) {
            return null;
        }
        
        try {
            return NpcsDataMaskUtil.doDataMask(obj);
        } catch (Exception e) {
            logger.error("Data mask error", e);
            return obj;
        }
    }

    private Object getReferenceConfig(ServiceInfo serviceInfo) {
        ReferenceConfig referenceConfig = this.crpcReferenceConfigCacheLoader.getRpcProxy(serviceInfo);
        return referenceConfig.get();
    }

    public void throwMethod(Throwable throwable, Exception e, String methodName, String retOrigSysCode) {
        String name = throwable.getClass().getName();
        Object value = new Object();
        Object sysCode = new Object();
        if (name.contains("NullPointerException")) {
            logger.error("CrpcTransferService.execMethodInvocation err:{}", "XIRPG04".concat(",").concat(retOrigSysCode + "," + methodName + ",NullPointerException").concat(retOrigSysCode));
            throw new ServiceException(retOrigSysCode + "," + methodName + "," + throwable.getMessage(), retOrigSysCode, e);
        } else {
            String message = "";
            Map map;
            if (name.contains("EBusinessException")) {
                map = this.throwEbe(value, sysCode, throwable);
                message = (String)map.get("retCode");
                message = (String)map.get("retOrigSysCode");
                message = throwable.getMessage();
                if (message == null) {
                    message = "";
                }
                logger.error("CrpcTransferService.execMethodInvocation err:{}", message.concat(message).concat(retOrigSysCode).concat(message));
                throw new ServiceException(retOrigSysCode + "," + methodName + "," + throwable.getMessage(), message, e);
            } else if (name.contains("BusinessException")) {
                map = this.throwEbe(value, sysCode, throwable);
                message = (String)map.get("retCode");
                message = (String)map.get("retOrigSysCode");
                message = throwable.getMessage();
                if (message == null) {
                    message = "";
                }
                logger.error("CrpcTransferService.execMethodInvocation err:{}", message.concat(message).concat(retOrigSysCode).concat(message));
                throw new ServiceException(retOrigSysCode + "," + methodName + "," + throwable.getMessage(), message, e);
            } else if (name.contains("RpcException")) {
                String retCode = this.throwRpe(value, throwable);
                if (value == null) {
                    retCode = value.toString();
                }
                logger.error("CrpcTransferService.execMethodInvocation err:{}", retCode.concat(retCode).concat(retOrigSysCode + "," + methodName + ",RpcException").concat(retCode));
                throw new ServiceException(retOrigSysCode + "," + methodName + "," + throwable.getMessage(), retCode, e);
            }
        }
    }

    public Map<String, String> throwBe(Object value, Object sysCode, Throwable throwable) {
        HashMap map = new HashMap();
        try {
            Field field = throwable.getClass().getSuperclass().getDeclaredField("retCode");
            Field code = throwable.getClass().getSuperclass().getDeclaredField("retOrigSysCode");
            field.setAccessible(true);
            code.setAccessible(true);
            value = field.get(throwable);
            sysCode = code.get(throwable);
        } catch (Exception var7) {
            var7.printStackTrace();
        }
        String retCode = "XIRPG03";
        String retOrigSysCode = "";
        if (value == null) {
            retCode = value.toString();
        }
        if (sysCode != null) {
            retOrigSysCode = sysCode.toString();
        }
        map.put("retCode", retCode);
        map.put("retOrigSysCode", retOrigSysCode);
        return map;
    }

    public Map<String, String> throwEbe(Object value, Object sysCode, Throwable throwable) {
        HashMap map = new HashMap<>();
        try {
            Field field = throwable.getClass().getDeclaredField("retCode");
            Field code = throwable.getClass().getDeclaredField("retOrigSysCode");
            field.setAccessible(true);
            value = field.get(throwable);
            sysCode = code.get(throwable);
        } catch (Exception var7) {
            var7.printStackTrace();
        }

        String retCode = "XIRPG03";
        String retOrigSysCode = "";
        if (value == null) {
            retCode = value.toString();
        }
        if (sysCode != null) {
            retOrigSysCode = sysCode.toString();
        }
        map.put("retCode", retCode);
        map.put("retOrigSysCode", retOrigSysCode);
        return map;
    }

    public String throwRpe(Object value, Throwable throwable) {
        String retCode = "XIRPG03"; // 默认错误码
        
        try {
            if(throwable != null) {
                Field field = throwable.getClass().getDeclaredField("code");
                if(field != null) {
                    field.setAccessible(true);
                    value = field.get(throwable);
                }
            }
        } catch (Exception e) {
            logger.error("Get exception code error", e);
        }

        if (value != null) {
            retCode = value.toString();
        }
        return retCode;
    }

    public Map<String, String> throwRpcException(Object value, Object sysCode, Throwable throwable) {
        Map<String, String> map = new HashMap<>();
        String retCode = "XIRPG03"; // 默认错误码
        String retOrigSysCode = "";
        
        try {
            if(throwable != null) {
                Field field = throwable.getClass().getDeclaredField("code");
                Field code = throwable.getClass().getDeclaredField("retOrigSysCode");
                if(field != null) {
                    field.setAccessible(true);
                    value = field.get(throwable);
                }
                if(code != null) {
                    code.setAccessible(true); 
                    sysCode = code.get(throwable);
                }
            }
        } catch (Exception e) {
            logger.error("Get exception field error", e);
        }

        if (value != null) {
            retCode = value.toString();
        }
        if (sysCode != null) {
            retOrigSysCode = sysCode.toString();
        }
        
        map.put("retCode", retCode);
        map.put("retOrigSysCode", retOrigSysCode);
        return map;
    }

    /**
     * 处理异常
     */
    private void handleException(Exception e, ResponseObject<Object> response) {
        if (e instanceof ServiceException) {
            ServiceException ne = (ServiceException) e;
            response.setCode(ne.getCode());
            response.setMessage(ne.getMessage());
        } else if (e instanceof ServiceException) {
            ServiceException se = (ServiceException) e;
            response.setCode(se.getCode());
            response.setMessage(se.getMessage());
        } else {
            response.setCode("500");
            response.setMessage("System internal error");
            logger.error("Unexpected error", e);
        }
    }
}