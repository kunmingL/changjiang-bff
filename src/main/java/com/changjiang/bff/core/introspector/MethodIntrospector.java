package com.changjiang.bff.core.introspector;

import com.changjiang.bff.annotation.ConfigurationGWClass;
import com.changjiang.bff.annotation.ConfigureMethod;
import com.changjiang.bff.annotation.ServiceConfig;
import com.changjiang.bff.core.ApiScanner;
import io.micrometer.common.util.StringUtils;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.Logger;
import org.springframework.stereotype.Component;
import org.springframework.beans.factory.annotation.Autowired;
import org.slf4j.LoggerFactory;

import javax.annotation.PostConstruct;
import java.util.concurrent.ConcurrentHashMap;

import java.lang.reflect.Method;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 方法内省器
 * 主要职责：
 * 1. 解析和管理服务方法信息
 * 2. 提供方法查找和实例获取
 * 3. 支持方法参数和返回值的类型处理
 *
 * 使用场景：
 * - 服务方法的动态调用
 * - 方法参数类型转换
 * - 方法实例的缓存管理
 *
 * 调用关系：
 * - 被TransferService调用获取方法信息
 * - 与ServiceApiScanner配合工作
 * - 调用Spring容器获取实例
 */
@Slf4j
@Component
public class MethodIntrospector {
    /**
     * API扫描器
     * 用于获取服务API的配置信息
     */
    @Autowired
    private ApiScanner apiScanner;

    protected  final Map<String, Map<Method, Object>> SPEC_METHOD_CONFIG_MAP = new ConcurrentHashMap<>();

    public  Set<String> URL_SET = Collections.newSetFromMap(new ConcurrentHashMap<>());

    public  Set<String> METHOD_SET = Collections.newSetFromMap(new ConcurrentHashMap<>());

    // 函数式接口，只能包含一个抽象方法
    @FunctionalInterface
    public interface MethodFilter {
        boolean matches(Method var1);
    }

    /**
     * 根据指定的条件过滤类的方法
     * 此方法通过接受一个类类型和一个方法过滤器，来筛选出该类中符合特定条件的方法
     * 主要用于在给定的类中查找特定的方法，例如公共方法、私有方法或具有特定注解的方法等
     *
     * @param beanType 要筛选方法的类类型这个参数指定了哪个类的方法将被筛选
     * @param methodFilter 方法过滤器接口，定义了筛选方法的条件这个参数决定了哪些方法会被筛选出来
     * @return 返回一个包含筛选后的方法列表的方法列表反映了筛选条件应用到类方法上的结果
     */
    private static List<Method> filterMethod(Class<?> beanType, MethodFilter methodFilter) {
        // 获取类声明的所有方法，包括公共、保护、默认访问和私有方法
        Method[] methods = beanType.getDeclaredMethods();
        // 将方法数组转换为列表，以便进行流操作
        List<Method> methodList = Arrays.asList(methods);
        // 使用流操作和方法过滤器筛选出符合特定条件的方法，并收集到一个新的列表中
        methodList = methodList.stream().filter((method -> methodFilter.matches(method))).collect(Collectors.toList());
        // 返回筛选后的包含符合条件的方法的列表
        return methodList;
    }

    /**
     * 根据给定的条件选择并处理指定对象中的方法
     * 此方法主要用于从一个配置类中筛选出符合条件的方法，并进行一系列的处理操作，
     * 包括构建请求URL、检查URL和方法名的唯一性，并将方法与其所在的类实例一起存入配置映射中
     *
     * @param beanObj 要处理的对象，通常是一个配置类的实例
     * @param methodFilter 方法过滤器，用于筛选符合条件的方法
     */
    public  void selectMethods(Object beanObj, MethodFilter methodFilter) {
        // 获取配置类的请求URL前缀
        String reqUrlPrefix = beanObj.getClass().getAnnotation(ServiceConfig.class).url();
        // 获取类中声明的所有方法
        Method[] methods = beanObj.getClass().getDeclaredMethods();
        if (methods == null || methods.length == 0) {
            return;
        }
        // 根据给定的方法过滤器筛选方法
        List<Method> filteredList = filterMethod(beanObj.getClass(), methodFilter);
        filteredList.forEach(method -> {
            // 创建一个映射，用于存储方法和其对应的实例
            Map<Method, Object> instanceMap = new HashMap<>();
            // 拼接请求URL
            String reqUrl = reqUrlPrefix.concat(method.getAnnotation(ConfigureMethod.class).reqUrl());


            // 检查URL是否重复
            if (!URL_SET.add(reqUrl)) {
                throw new RuntimeException("url 配置重复");
            }
            // 检查方法名是否重复
            if (!METHOD_SET.add(method.getName())) {
                throw new RuntimeException("method 配置重复");
            }
            // 将方法和实例存入映射
            instanceMap.put(method, beanObj);
            // 将请求URL和包含方法及实例的映射存入配置映射中
            SPEC_METHOD_CONFIG_MAP.put(reqUrl, instanceMap);
        });
    }

    /**
     * 获取服务实例和方法
     * 主要功能：
     * 1. 查找方法信息
     * 2. 获取服务实例
     * 3. 处理缓存逻辑
     *
     * @param uri 服务URI
     * @return 方法和实例的映射
     */
    public Map<Method, Object> getServiceInstanceByMethod(String uri) {
        if (StringUtils.isNotBlank(uri)) {
            return SPEC_METHOD_CONFIG_MAP.get(uri);
        } else {
            return null;
        }
    }

    /**
     * 在组件初始化完成后调用此方法，确保在 ApiScanner 初始化之后执行
     */
    @PostConstruct
    public void init() {
        log.info("Initializing MethodIntrospector...");

        // apiScanner.getApiRegistry().forEach(cls -> {
        //     try {
        //         selectMethods(cls, method -> method.isAnnotationPresent(ConfigureMethod.class));
        //     } catch (Exception e) {
        //         log.error("Failed to initialize MethodIntrospector for class: " + cls.getClass().getName(), e);
        //     }
        // });

        log.info("MethodIntrospector initialization completed.");
    }

}
