package com.changjing.bff.core.introspector;

import com.changjing.bff.annotation.ConfigurationGWClass;
import com.changjing.bff.annotation.ConfigureMethod;
import io.micrometer.common.util.StringUtils;
import org.springframework.stereotype.Component;

import java.lang.reflect.Method;
import java.util.*;
import java.util.stream.Collectors;

@Component
public class MethodIntrospector {

    protected static final Map<String, Map<Method, Object>> SPEC_METHOD_CONFIG_MAP = new HashMap<>();
    public static Set<String> URL_SET = new HashSet<>();
    public static Set<String> METHOD_SET = new HashSet<>();

    // 函数式接口，只能包含一个抽象方法
    @FunctionalInterface
    public interface MethodFilter {
        boolean matches(Method var1);
    }

    private static List<Method> filterMethod(Class<?> beanType, MethodFilter methodFilter) {
        Method[] methods = beanType.getDeclaredMethods();
        List<Method> methodList = Arrays.asList(methods);
        methodList = methodList.stream().filter((method -> methodFilter.matches(method))).collect(Collectors.toList());
        return methodList;
    }

    public static void selectMethods(Object beanObj, MethodFilter methodFilter) {
        String reqUrlPrefix = beanObj.getClass().getAnnotation(ConfigurationGWClass.class).reqUrlPrefix();
        Method[] methods = beanObj.getClass().getDeclaredMethods();
        if (methods == null || methods.length == 0) {
            return;
        }
        List<Method> filteredList = filterMethod(beanObj.getClass(), methodFilter);
        filteredList.forEach(method -> {
            Map<Method, Object> instanceMap = new HashMap<>();
            String reqUrl = reqUrlPrefix.concat(method.getAnnotation(ConfigureMethod.class).reqUrl());
            if (!URL_SET.add(reqUrl)) {
                throw new RuntimeException("url 配置重复");
            }
            if (!METHOD_SET.add(method.getName())) {
                throw new RuntimeException("method 配置重复");
            }
            instanceMap.put(method, beanObj);
            SPEC_METHOD_CONFIG_MAP.put(reqUrl, instanceMap);
        });
    }

    public Map<Method, Object> getServiceInstanceByMethod(String indexUrl) {
        if (StringUtils.isNotBlank(indexUrl)) {
            return SPEC_METHOD_CONFIG_MAP.get(indexUrl);
        } else {
            return null;
        }
    }
}
