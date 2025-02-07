package com.changjing.bff.core;

import com.changjing.bff.common.ServiceConfig;
import lombok.Data;

import java.lang.reflect.Method;

@Data
public class ServiceApiInfo {
    private Method method;
    private Class<?> targetClass;
    private ServiceConfig config;
    private Object instance;
} 