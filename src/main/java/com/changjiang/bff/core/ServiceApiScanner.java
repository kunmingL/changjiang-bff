// package com.changjiang.bff.core;
//
// import org.springframework.beans.factory.annotation.Autowired;
// import org.springframework.context.ApplicationContext;
// import org.springframework.context.ApplicationListener;
// import org.springframework.context.event.ContextRefreshedEvent;
// import org.springframework.stereotype.Component;
// import org.springframework.stereotype.Service;
// import org.springframework.util.AopUtils;
// import org.springframework.web.bind.annotation.RequestMapping;
// import org.slf4j.Logger;
// import org.slf4j.LoggerFactory;
//
// import java.lang.reflect.Method;
// import java.util.concurrent.ConcurrentHashMap;
// import java.util.Map;
//
// /**
//  * 服务API扫描器
//  * 主要职责：
//  * 1. 扫描并注册所有服务API
//  * 2. 管理API信息的缓存
//  * 3. 提供API信息的查询服务
//  *
//  * 使用场景：
//  * - 系统启动时的API扫描
//  * - 运行时的API信息查询
//  * - 服务调用时的方法查找
//  *
//  * 调用关系：
//  * - 被Spring容器管理和初始化
//  * - 被TransferService调用获取API信息
//  * - 与ServiceApiInfo配合提供API元数据
//  */
// @Component
// public class ServiceApiScanner implements ApplicationListener<ContextRefreshedEvent> {
//     private static final Logger logger = LoggerFactory.getLogger(ServiceApiScanner.class);
//
//     private final Map<String, ServiceApiInfo> apiInfoCache = new ConcurrentHashMap<>();
//
//     @Autowired
//     private MethodParameterHandler methodParameterHandler;
//
//     /**
//      * 在Spring容器刷新时执行扫描
//      */
//     @Override
//     public void onApplicationEvent(ContextRefreshedEvent event) {
//         scanServiceApis(event.getApplicationContext());
//     }
//
//     /**
//      * 扫描服务API
//      */
//     private void scanServiceApis(ApplicationContext context) {
//         // 扫描带有@Service注解的类
//         Map<String, Object> serviceMap = context.getBeansWithAnnotation(Service.class);
//
//         for (Object service : serviceMap.values()) {
//             Class<?> serviceClass = AopUtils.getTargetClass(service);
//
//             // 处理类中的所有公共方法
//             for (Method method : serviceClass.getMethods()) {
//                 if (isApiMethod(method)) {
//                     registerApiMethod(service, method);
//                 }
//             }
//         }
//
//         logger.info("Scanned {} service APIs", apiInfoCache.size());
//     }
//
//     /**
//      * 判断方法是否为API方法
//      */
//     private boolean isApiMethod(Method method) {
//         // 排除Object类的方法
//         if (method.getDeclaringClass() == Object.class) {
//             return false;
//         }
//
//         // 检查方法注解
//         return method.isAnnotationPresent(ApiOperation.class) ||
//                method.isAnnotationPresent(RequestMapping.class);
//     }
//
//     /**
//      * 注册API方法
//      */
//     private void registerApiMethod(Object service, Method method) {
//         ServiceApiInfo apiInfo = new ServiceApiInfo();
//         apiInfo.setInstance(service);
//         apiInfo.setMethod(method);
//         apiInfo.setParameterHandler(methodParameterHandler);
//
//         // 获取API路径
//         String apiPath = getApiPath(method);
//         apiInfoCache.put(apiPath, apiInfo);
//
//         logger.debug("Registered API: {}", apiPath);
//     }
//
//     /**
//      * 获取API路径
//      */
//     private String getApiPath(Method method) {
//         // 优先使用RequestMapping注解
//         RequestMapping requestMapping = method.getAnnotation(RequestMapping.class);
//         if (requestMapping != null && requestMapping.value().length > 0) {
//             return requestMapping.value()[0];
//         }
//
//         // 使用ApiOperation注解
//         ApiOperation apiOperation = method.getAnnotation(ApiOperation.class);
//         if (apiOperation != null) {
//             return apiOperation.value();
//         }
//
//         // 默认使用类名+方法名
//         return method.getDeclaringClass().getSimpleName() + "." + method.getName();
//     }
//
//     /**
//      * 获取API信息
//      */
//     public ServiceApiInfo getApiInfo(String uri) {
//         return apiInfoCache.get(uri);
//     }
// }