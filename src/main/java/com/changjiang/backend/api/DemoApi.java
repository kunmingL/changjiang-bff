package com.changjiang.backend.api;


import com.changjiang.bff.annotation.ServiceConfig;
import com.changjiang.bff.constants.SrvChannel;

public class DemoApi {

    @ServiceConfig(
        registryId = "demo.api.test",
        url = "/api/demo/test",
        channel = {SrvChannel.PC, SrvChannel.MOBILE}
    )
    public Object testApi(Object request) {
        // 实际处理逻辑
        return null;
    }
} 