package com.changjiang.bff.exception;

public class NpcgwSWException extends RuntimeException {
    private String code;
    
    public NpcgwSWException(String code) {
        this.code = code;
    }
    
    public String getCode() {
        return code;
    }
} 