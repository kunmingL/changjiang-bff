package com.changjiang.bff.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Data
@Component
@ConfigurationProperties(prefix = "crpc")
public class CrpcProperties {
    private int timeout = 30000;
    private int retries = 2;
    private String cluster = "failover";
    private String loadbalance = "random";
    private String protocol = "dubbo";
    private String address = "127.0.0.1:20880";
} 