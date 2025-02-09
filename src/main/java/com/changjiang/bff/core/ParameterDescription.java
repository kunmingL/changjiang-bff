package com.changjiang.bff.core;
import lombok.Data;

import java.util.List;

/**
 * 参数描述类
 * 主要职责：
 * 1. 存储参数的描述信息
 * 2. 支持参数验证规则
 * 3. 用于API文档生成
 */
@Data
public class ParameterDescription {
    /** 参数名称 */
    private String name;
    
    /** 参数类型 */
    private Class<?> type;
    
    /** 参数描述 */
    private String description;
    
    /** 是否必填 */
    private boolean required;
    
    /** 默认值 */
    private String defaultValue;
    
    /** 验证规则 */
    private List<String> validations;
} 