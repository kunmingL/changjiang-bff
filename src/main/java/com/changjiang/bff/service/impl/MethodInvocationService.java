package com.changjiang.bff.service.impl;

import com.alibaba.fastjson2.JSONObject;

public interface MethodInvocationService {

    <P extends JSONObject,T extends Object> T invokeService(String url, P params) throws Exception;

}
