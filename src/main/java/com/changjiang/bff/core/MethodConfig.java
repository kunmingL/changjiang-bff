package com.changjiang.bff.core;

import lombok.Data;

@Data
public class MethodConfig {
    private String methodName;
    private int timeout;
    private int retries;
} 