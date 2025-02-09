package com.changjiang.bff.core;

import lombok.Data;

@Data
public class RegistryConfig {
    private String address;
    private String protocol;
    private RegistryConfig backupRegistry;
} 