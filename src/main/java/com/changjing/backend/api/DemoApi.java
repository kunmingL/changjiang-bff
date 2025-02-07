package com.changjing.backend.api;

import com.changjing.bff.common.ServiceConfig;
import com.changjing.bff.common.SrvChannel;

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