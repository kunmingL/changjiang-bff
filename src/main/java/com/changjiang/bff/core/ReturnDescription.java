package com.changjiang.bff.core;

import lombok.Data;

import java.util.List;

/**
 * 返回值描述类
 * 主要职责:
 * 1. 描述API返回值的类型和结构
 * 2. 支持泛型返回值的描述
 * 3. 用于API文档生成
 */
@Data
public class ReturnDescription {
    /** 返回值类型 */
    private Class<?> type;
    
    /** 泛型类型参数 */
    private List<Class<?>> genericTypes;
    
    /** 返回值描述 */
    private String description;
    
    /** 示例值 */
    private String example;
    
    /** 是否为集合类型 */
    private boolean isCollection;
    
    /** 是否为分页结果 */
    private boolean isPageResult;
} 