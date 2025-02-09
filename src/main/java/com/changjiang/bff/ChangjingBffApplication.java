package com.changjiang.bff;

import com.changjiang.bff.config.ServiceScanProperties;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.ComponentScan;

@SpringBootApplication
@EnableConfigurationProperties(ServiceScanProperties.class)
@ComponentScan(basePackages = {
    "com.changjiang.bff.core",
    "com.changjiang.bff.config",
    "com.changjiang.bff.util",
    "com.changjiang.grpc"
})
public class ChangjingBffApplication {

    public static void main(String[] args) {
        SpringApplication.run(ChangjingBffApplication.class, args);
    }

}
