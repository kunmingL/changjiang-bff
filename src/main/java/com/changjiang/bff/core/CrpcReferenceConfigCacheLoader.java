package com.changjiang.bff.core;

import com.changjiang.bff.config.CrpcProperties;
import com.changjiang.bff.entity.ServiceInfo;
import com.changjiang.bff.exception.ServiceException;
import com.google.common.cache.CacheLoader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * CRPC引用配置缓存加载器
 * 主要职责：
 * 1. 管理CRPC服务引用的缓存
 * 2. 提供服务代理对象的创建和获取
 * 3. 优化服务引用的性能
 * 
 * 使用场景：
 * - 服务代理对象的缓存管理
 * - 动态服务引用加载
 * - 性能优化
 * 
 * 调用关系：
 * - 被CrpcTransferService调用获取服务代理
 * - 调用RPC框架创建服务代理
 * - 与ServiceInfo配合完成服务发现
 */
@Component
public class CrpcReferenceConfigCacheLoader extends CacheLoader<ServiceInfo, Object> {

    private static final Logger logger = LoggerFactory.getLogger(CrpcReferenceConfigCacheLoader.class);

    /** 服务引用缓存 */
    private final Map<String, ReferenceConfig<?>> referenceConfigCache = new ConcurrentHashMap<>();
    
    /** 服务代理缓存 */
    private final Map<String, Object> proxyCache = new ConcurrentHashMap<>();
    
    @Autowired
    private CrpcProperties crpcProperties;

    @Autowired
    private NpcsgptwPropertyUtils npcgwPropertyUtils;

    private String backupId;
    private String backupProtocol;
    private String backupAddress;

    /** 
     * 引用配置缓存
     * 存储服务引用的配置信息
     * key: 服务信息, value: 引用配置
     */
    public static ConcurrentHashMap<String, ReferenceConfig> REFERENCE_CONFIG_MAP = new ConcurrentHashMap<>();

    public CrpcReferenceConfigCacheLoader() {
        // Constructor logic if any
    }

    public void initBackupConfigs() {
        CrcpBFFConfig backupConfig = this.npcgwPropertyUtils.getBackupProperties("cpc.registries.nacosconf");
        if (backupConfig != null) {
            this.backupId = backupConfig.getRegistryId();
            this.backupProtocol = backupConfig.getProtocol();
            this.backupAddress = backupConfig.getAddress();
        }
    }

    public static ConcurrentHashMap<String, ReferenceConfig> getReferenceConfigMap() {
        return REFERENCE_CONFIG_MAP;
    }

    /**
     * 获取服务代理
     */
    public Object getProxy(String interfaceName) throws ServiceException {
        try {
            return proxyCache.computeIfAbsent(interfaceName, k -> {
                try {
                    ReferenceConfig<?> referenceConfig = getReferenceConfig(interfaceName);
                    return referenceConfig.get();
                } catch (Exception e) {
                    logger.error("Failed to create proxy for interface: " + interfaceName, e);
                    throw new ServiceException("PROXY_ERROR", "创建服务代理失败", e);
                }
            });
        } catch (Exception e) {
            logger.error("Get proxy error", e);
            throw new ServiceException("PROXY_ERROR", "获取服务代理失败", e);
        }
    }
    
    /**
     * 获取引用配置
     */
    private ReferenceConfig<?> getReferenceConfig(String interfaceName) throws ClassNotFoundException {
        return referenceConfigCache.computeIfAbsent(interfaceName, k -> {
            try {
                ReferenceConfig<?> referenceConfig = new ReferenceConfig<>();
                //referenceConfig.setInterface(Class.forName(interfaceName));
                
                // 设置超时和重试配置
                referenceConfig.setTimeout(crpcProperties.getTimeout());
                referenceConfig.setRetries(crpcProperties.getRetries());
                
                // 设置集群策略
                if (!StringUtils.isEmpty(crpcProperties.getCluster())) {
                    referenceConfig.setCluster(crpcProperties.getCluster());
                }
                
                // 设置负载均衡策略
                if (!StringUtils.isEmpty(crpcProperties.getLoadbalance())) {
                    referenceConfig.setLoadbalance(crpcProperties.getLoadbalance());
                }
                
                return referenceConfig;
            } catch (Exception e) {
                logger.error("Interface not found: " + interfaceName, e);
                throw new ServiceException("CLASS_NOT_FOUND", "服务接口不存在", e);
            }
        });
    }
    
    /**
     * 清除缓存
     */
    public void clearCache() {
        referenceConfigCache.clear();
        proxyCache.clear();
        REFERENCE_CONFIG_MAP.clear();
        logger.info("All caches cleared");
    }

    @Override
    public Object load(ServiceInfo serviceInfo) throws Exception {
        return getRpcProxy(serviceInfo);
    }

    public ReferenceConfig getRpcProxy(ServiceInfo serviceInfo) {
        String itfName = serviceInfo.getInterfaceClass().getName();
        ReferenceConfig clientProxy = REFERENCE_CONFIG_MAP.get(itfName);
        if (clientProxy == null) {
            clientProxy = this.getReferenceConfig(serviceInfo.getAddress(), 
                                                serviceInfo.getProtocol(), 
                                                serviceInfo.getInterfaceClass(), 
                                                serviceInfo.getMethodName(), 
                                                serviceInfo.getTimeoutMills());
            REFERENCE_CONFIG_MAP.put(itfName, clientProxy);
        }
        return clientProxy;
    }

    public ReferenceConfig getReferenceConfig(String address, String protocol, Class<?> interfaceClass, String methodName, int timeoutMills) {
        ReferenceConfig referenceConfig = REFERENCE_CONFIG_MAP.get(interfaceClass.getName());
        if (referenceConfig == null) {
            referenceConfig = new ReferenceConfig();
            
            // 设置方法配置
            List<MethodConfig> methods = new ArrayList<>();
            MethodConfig methodConfig = new MethodConfig();
            methodConfig.setMethodName(methodName);
            methods.add(methodConfig);
            referenceConfig.setMethods(methods);
            
            // 设置注册中心配置
            RegistryConfig registry = new RegistryConfig();
            registry.setAddress(address);
            registry.setProtocol(protocol);
            
            // 设置备份注册中心
            if ("mesh".equalsIgnoreCase(protocol)) {
                RegistryConfig backUp = new RegistryConfig();
                backUp.setAddress(this.backupAddress);
                backUp.setProtocol(this.backupProtocol);
                registry.setBackupRegistry(backUp);
            }
            
            referenceConfig.setRegistry(registry);
            referenceConfig.setInterfaceClass(interfaceClass);
            
            // 设置超时时间
            if (timeoutMills < 0) {
                timeoutMills = 30000;
            }
            referenceConfig.setTimeout(timeoutMills);
            
            REFERENCE_CONFIG_MAP.put(interfaceClass.getName(), referenceConfig);
        }
        return referenceConfig;
    }

    public Map loadAll(Iterable keys) throws Exception {
        return null;
    }
}
