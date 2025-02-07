# Changjing BFF Service

## 项目简介
这是一个基于 Spring Boot 的 BFF (Backend For Frontend) 服务，主要用于处理前端请求并转发到后端微服务。支持通过注解扫描后端服务 API 并进行智能转发。

## 技术栈
- Java 17
- Spring Boot 3.2.3
- FastJSON
- Lombok
- Reflections

## 核心功能
1. 注解扫描：通过 @ServiceConfig 注解扫描后端服务 API
2. 智能转发：根据注解配置进行请求转发
3. 统一响应：统一的响应格式和错误处理
4. 多渠道支持：支持 PC、移动等多个渠道

## ServiceConfig 注解说明
```java
@ServiceConfig(
    registryId = "服务注册ID",
    url = "服务URL",
    channel = {SrvChannel.PC, SrvChannel.MOBILE},
    dealResType = false,
    dataMask = false,
    methodName = "方法名",
    specClassReference = {},
    referField = ""
)
```

## 配置说明
1. 应用配置 (application.properties)
   - 服务端口: 8081
   - 后端服务地址: service.backend.url
   - API扫描包路径: service.scan.packages

## 使用示例
1. 在后端服务 API 上添加 @ServiceConfig 注解
2. 配置扫描包路径
3. 启动服务，自动扫描并注册 API

## 启动说明
1. 确保 JDK 17 已安装
2. 配置 application.properties
3. 执行 `mvn spring-boot:run`
4. 服务将在 8081 端口启动

## 注意事项
1. 确保后端服务 API 包在扫描路径中
2. 注解配置需要合理设置
3. 建议添加适当的安全措施

## ServiceConfig 注解使用示例
```java
@ServiceConfig(
    registryId = "demo.api.test",
    url = "/api/demo/test",
    channel = {SrvChannel.PC, SrvChannel.MOBILE}
)
public Object testApi(Object request) {
    // 实际处理逻辑
    return null;
}
```

## 使用说明
1. 在后端服务方法上添加 @ServiceConfig 注解
2. 配置 registryId 或 url 用于标识服务
3. 配置 channel 指定支持的客户端类型
4. 实现方法逻辑处理请求参数并返回结果