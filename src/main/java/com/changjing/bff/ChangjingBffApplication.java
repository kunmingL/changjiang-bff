package com.changjing.bff;

import com.changjiang.grpc.annotation.EnableGrpcService;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
@EnableGrpcService
public class ChangjingBffApplication {

    public static void main(String[] args) {
        SpringApplication.run(ChangjingBffApplication.class, args);
    }

}
