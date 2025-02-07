
package com.changjing.bff.core;

import com.changjiang.bff.annotation.ServiceConfig;
import org.reflections.Reflections;
import org.reflections.scanners.MethodAnnotationsScanner;
import org.reflections.util.ConfigurationBuilder;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.lang.reflect.Method;
import java.nio.file.Paths;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class ServiceApiScanner {

    private final ConcurrentHashMap<String, ServiceApiInfo> apiRegistry = new ConcurrentHashMap<>();

    /**
     * 扫描外部JAR文件中的APIs
     * 该方法主要用于扫描位于外部JAR文件中的服务配置方法，并执行相应的处理
     *
     * @param jarFilePaths JAR文件的路径列表，不能为空或空字符串
     * @param basePackage 基础包名，用于确定扫描的包范围，不能为空或空字符串
     *
     * @throws IllegalArgumentException 如果jarFilePaths或basePackage为空或空字符串，则抛出此异常
     * @throws RuntimeException 如果读取JAR文件失败，则抛出此异常
     */
    public void scanExternalJarApis(List<String> jarFilePaths, String basePackage) {
        // 检查输入参数的有效性
        if (jarFilePaths == null || jarFilePaths.isEmpty()) {
            throw new IllegalArgumentException("jarFilePaths cannot be null or empty");
        }
        if (basePackage == null || basePackage.trim().isEmpty()) {
            throw new IllegalArgumentException("basePackage cannot be null or empty");
        }

        for (String jarFilePath : jarFilePaths) {
            try {
                // 创建ConfigurationBuilder并指定JAR文件路径
                ConfigurationBuilder config = new ConfigurationBuilder()
                        .addUrls(Paths.get(jarFilePath).toUri().toURL())
                        .setScanners(new MethodAnnotationsScanner());

                // 初始化Reflections对象
                Reflections reflections = new Reflections(config);

                // 获取所有标注了ServiceConfig注解的方法
                Set<Method> methods = reflections.getMethodsAnnotatedWith(ServiceConfig.class);

                // 继续执行与scanApis相同的操作
                for (Method method : methods) {
                    processMethod(method);
                }
            } catch (IOException e) {
                throw new RuntimeException("Failed to read JAR file: " + jarFilePath, e);
            }
        }
    }

    /**
     * 处理方法，将其配置为服务接口
     * 该方法首先获取方法上ServiceConfig注解的实例，然后创建ServiceApiInfo对象
     * 接着，它获取方法所属类的Class对象，并创建该类的实例
     * 如果实例成功创建，它将实例设置到apiInfo中，并根据注解配置确定服务的键
     * 最后，将该键和apiInfo对象存入apiRegistry中
     *
     * @param method 要处理的方法，用于提取服务配置信息
     */
    private void processMethod(Method method) {
        // 获取方法上的ServiceConfig注解实例
        ServiceConfig configAnnotation = method.getAnnotation(ServiceConfig.class);
        // 根据方法和注解创建ServiceApiInfo对象
        ServiceApiInfo apiInfo = createApiInfo(method, configAnnotation);

        // 获取方法所属类的Class对象
        Class<?> targetClass = method.getDeclaringClass();
        // 创建该类的实例
        Object instance = createInstance(targetClass);
        // 如果实例成功创建，将其设置到apiInfo中
        if (instance != null) {
            apiInfo.setInstance(instance);

            // 根据注解配置确定服务的键
            String key = !configAnnotation.registryId().isEmpty() ? configAnnotation.registryId() : configAnnotation.url();
            // 将键和apiInfo对象存入apiRegistry中
            apiRegistry.put(key, apiInfo);
        }
    }

    /**
     * 创建并返回一个ServiceApiInfo对象
     * 该方法用于封装服务的API信息，包括方法和配置注解
     *
     * @param method 反射获取的方法对象，代表服务中的一个方法
     * @param configAnnotation 服务配置的注解，包含服务的配置信息
     * @return 返回一个ServiceApiInfo对象，其中包含了方法和配置注解的信息
     */
    private ServiceApiInfo createApiInfo(Method method, ServiceConfig configAnnotation) {
        // 实例化ServiceApiInfo对象
        ServiceApiInfo apiInfo = new ServiceApiInfo();
        // 设置方法信息
        apiInfo.setMethod(method);
        // 设置服务配置注解
        apiInfo.setConfig(configAnnotation);
        // 返回封装好的ServiceApiInfo对象
        return apiInfo;
    }

    /**
     * 创建指定类的实例
     * 该方法使用反射来创建一个指定类的实例它尝试通过调用无参构造函数来创建这个实例
     * 如果在实例化过程中遇到任何问题，比如没有找到无参构造函数或者构造函数是不可访问的，
     * 方法会打印错误信息并返回null
     *
     * @param targetClass 目标类，表示想要创建实例的类必须是已经加载的类
     * @return Object 返回创建的实例对象如果创建失败，返回null
     */
    private Object createInstance(Class<?> targetClass) {
        try {
            // 使用反射调用目标类的无参构造方法创建实例
            return targetClass.getDeclaredConstructor().newInstance();
        } catch (Exception e) {
            // 实例化失败时打印错误信息
            System.err.println("Failed to create instance for " + targetClass.getName() + ": " + e.getMessage());
            return null;
        }
    }


    /**
     * 根据给定的键获取API信息
     * 此方法用于从API注册表中检索与指定键关联的服务API信息
     *
     * @param key API信息的唯一键
     * @return ServiceApiInfo对象，包含服务的API信息如果键不存在，则返回null
     */
    public ServiceApiInfo getApiInfo(String key) {
        return apiRegistry.get(key);
    }
}