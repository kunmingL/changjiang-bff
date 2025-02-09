package com.changjiang.bff.enums;
/**
 * 脱敏类型枚举
 * 主要职责：
 * 1. 定义支持的脱敏类型
 * 2. 提供脱敏规则说明
 */
public enum MaskType {
    DEFAULT,    // 默认脱敏
    PHONE,      // 手机号脱敏
    ID_CARD,    // 身份证脱敏
    EMAIL,      // 邮箱脱敏
    BANK_CARD   // 银行卡脱敏
} 