
package com.changjing.bff.config;

import com.changjing.bff.core.ApiScanner;
import com.changjing.bff.core.ServiceApiScanner;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

import javax.annotation.PostConstruct;
import java.util.Arrays;
import java.util.List;

@Configuration
public class ServiceScanConfig {

    @Value("${service.scan.packages}")
    private String[] scanPackages;

    @Value("${service.scan.jar.files}")
    private String jarFiles;

    private final ServiceApiScanner apiScanner;

    @Autowired
    public ServiceScanConfig(ServiceApiScanner apiScanner) {
        this.apiScanner = apiScanner;
    }

    @PostConstruct
    public void init() {
        List<String> jarFilePaths = Arrays.asList(jarFiles.split(","));
        String basePackage = "com.example.basepackage"; // 替换为实际的基础包名
        apiScanner.scanExternalJarApis(jarFilePaths, basePackage);
    }
}