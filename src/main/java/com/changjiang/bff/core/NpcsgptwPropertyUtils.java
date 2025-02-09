package com.changjiang.bff.core;

import org.springframework.context.EnvironmentAware;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.Environment;
import org.springframework.core.env.PropertySource;
import org.springframework.core.env.PropertyResolver;
import org.springframework.core.env.EnumerablePropertySource;
import org.springframework.util.StringUtils;
import org.springframework.stereotype.Component;

import java.util.*;

/**
 * 配置属性工具类
 * 主要职责：
 * 1. 管理系统配置属性
 * 2. 提供配置信息的访问接口
 * 3. 支持配置的动态加载
 * 
 * 使用场景：
 * - 系统配置管理
 * - 服务参数配置
 * - 环境变量处理
 * 
 * 调用关系：
 * - 被Spring容器管理
 * - 被CrcpBFFConfig使用
 * - 与Environment配合使用
 */
@Component
public class NpcsgptwPropertyUtils implements EnvironmentAware {
    /** 
     * 环境配置对象
     * 用于访问系统环境变量
     */
    private ConfigurableEnvironment environment;

    public NpcsgptwPropertyUtils(ConfigurableEnvironment environment) {
        this.environment = environment;
    }

    @Override
    public void setEnvironment(Environment environment) {
        this.environment = (ConfigurableEnvironment) environment;
    }

    public CrcpBFFConfig getBackupProperties(String backupAlias) {
        Map<String, Object> sourceConfigs = this.getSourceConfigs(backupAlias);
        String appGroupName = backupAlias.substring(backupAlias.lastIndexOf(".") + 1);
        CrcpBFFConfig crcpBFFConfig = new CrcpBFFConfig();
        crcpBFFConfig.setAddress((String) sourceConfigs.get(".address"));
        crcpBFFConfig.setProtocol((String) sourceConfigs.get(".protocol"));
        crcpBFFConfig.setRegistryId(appGroupName);
        return crcpBFFConfig;
    }

    public Map<String, CrcpBFFConfig> getPropertiesAppGroup(String regiPrefix) {
        if (!regiPrefix.endsWith(".")) {
            regiPrefix = regiPrefix + ".";
        }

        Map<String, CrcpBFFConfig> crcpConfigMap = new HashMap<>();
        Map<String, Object> sourceConfigs = this.getSourceConfigs(regiPrefix);
        Set<String> appGroupName = new LinkedHashSet<>();
        Iterator<String> var5 = sourceConfigs.keySet().iterator();

        String eachApp;
        while (var5.hasNext()) {
            eachApp = var5.next();
            int offset = eachApp.indexOf(".");
            if (offset > 0) {
                String appName = eachApp.substring(0, offset);
                appGroupName.add(appName);
            }
        }

        var5 = appGroupName.iterator();

        while (var5.hasNext()) {
            eachApp = var5.next();
            CrcpBFFConfig crcpBFFConfig = new CrcpBFFConfig();
            crcpBFFConfig.setAddress((String) sourceConfigs.get(eachApp + ".address"));
            crcpBFFConfig.setProtocol((String) sourceConfigs.get(eachApp + ".protocol"));
            if (StringUtils.hasText((String) sourceConfigs.get(eachApp + ".timeout"))) {
                crcpBFFConfig.setTimeoutUtils(Integer.parseInt((String) sourceConfigs.get(eachApp + ".timeout")));
            }
            crcpBFFConfig.setRegistryId(eachApp);
            crcpConfigMap.put(eachApp, crcpBFFConfig);
        }
        return crcpConfigMap;
    }

    /**
     * 获取配置属性
     * 主要功能：
     * 1. 解析配置前缀
     * 2. 获取指定属性值
     * 3. 处理属性占位符
     * 
     * @param prefix 配置前缀
     * @return 配置属性映射
     */
    private Map<String, Object> getSourceConfigs(String prefix) {
        Map<String, Object> propertyMap = new LinkedHashMap<>();
        Iterable<PropertySource<?>> propertySources = this.environment.getPropertySources();
        PropertyResolver propertyResolver = this.environment;
        Iterator<PropertySource<?>> iterator = propertySources.iterator();

        while (true) {
            PropertySource<?> source;
            do {
                if (!iterator.hasNext()) {
                    return Collections.unmodifiableMap(propertyMap);
                }
                source = iterator.next();
            } while (!(source instanceof EnumerablePropertySource));

            String[] propertyNames = ((EnumerablePropertySource<?>) source).getPropertyNames();
            for (String name : propertyNames) {
                if (!propertyMap.containsKey(name) && name.startsWith(prefix)) {
                    String splitName = name.substring(prefix.length());
                    if (!propertyMap.containsKey(splitName) && (splitName.contains(".protocol") || splitName.contains(".address") || splitName.contains(".timeout"))) {
                        Object value = source.getProperty(name);
                        if (value instanceof String) {
                            value = propertyResolver.resolvePlaceholders((String) value);
                        }
                        propertyMap.put(splitName, value);
                    }
                }
            }
        }
    }
}
