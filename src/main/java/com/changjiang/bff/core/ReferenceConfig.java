package com.changjiang.bff.core;

import lombok.Data;
import java.util.List;

@Data
public class ReferenceConfig<T> {
    private Class<T> interfaceClass;
    private int timeout;
    private int retries;
    private String cluster;
    private String loadbalance;
    private List<MethodConfig> methods;
    private RegistryConfig registry;
    
    public T get() {
        // 实际实现应该调用RPC框架的代理创建逻辑
        return null;
    }
} 