package com.changjiang.bff.util;

import com.changjiang.bff.annotation.DataMask;
import com.changjiang.bff.enums.MaskType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.StringUtils;

import java.lang.reflect.Field;
import java.util.Collection;
import java.util.Map;
import java.util.stream.Collectors;

import static com.fasterxml.jackson.databind.jsonFormatVisitors.JsonValueFormat.EMAIL;
import static com.fasterxml.jackson.databind.jsonFormatVisitors.JsonValueFormat.PHONE;

/**
 * 数据脱敏工具类
 * 主要职责：
 * 1. 处理敏感数据的脱敏
 * 2. 提供多种脱敏规则
 * 3. 支持自定义脱敏策略
 * 
 * 使用场景：
 * - 用户信息脱敏
 * - 金融数据保护
 * - 隐私信息处理
 * 
 * 调用关系：
 * - 被MethodInvocationService调用
 * - 被ResponseObject使用
 * - 与DataMaskAnnotation配合使用
 */
public class NpcsDataMaskUtil {
    /** 
     * 日志记录器
     * 用于记录脱敏处理过程
     */
    private static final Logger logger = LoggerFactory.getLogger(NpcsDataMaskUtil.class);
    
    /**
     * 执行数据脱敏
     */
    public static Object doDataMask(Object obj) {
        if (obj == null) {
            return null;
        }

        try {
            // 处理基本类型
            if (obj instanceof String || obj.getClass().isPrimitive()) {
                return obj;
            }

            // 处理集合类型
            if (obj instanceof Collection) {
                return ((Collection<?>) obj).stream()
                    .map(NpcsDataMaskUtil::doDataMask)
                    .collect(Collectors.toList());
            }

            // 处理Map类型
            if (obj instanceof Map) {
                return ((Map<?, ?>) obj).entrySet().stream()
                    .collect(Collectors.toMap(
                        Map.Entry::getKey,
                        e -> doDataMask(e.getValue())
                    ));
            }

            // 处理对象类型
            return maskObject(obj);

        } catch (Exception e) {
            logger.error("Data mask error", e);
            return obj;
        }
    }

    /**
     * 处理对象脱敏
     */
    private static Object maskObject(Object obj) throws Exception {
        Class<?> clazz = obj.getClass();
        Object maskedObj = clazz.getDeclaredConstructor().newInstance();

        // 处理所有字段
        for (Field field : clazz.getDeclaredFields()) {
            field.setAccessible(true);
            Object value = field.get(obj);

            // 检查是否需要脱敏
            DataMask maskAnnotation = field.getAnnotation(DataMask.class);
            if (maskAnnotation != null && value instanceof String) {
                value = maskField((String) value, maskAnnotation.type());
            } else {
                value = doDataMask(value);
            }

            field.set(maskedObj, value);
        }

        return maskedObj;
    }

    /**
     * 根据脱敏类型处理字段值
     */
    private static String maskField(String value, MaskType type) {
        if (StringUtils.isEmpty(value)) {
            return value;
        }

        switch (type) {
            case PHONE:
                return maskPhone(value);
            case ID_CARD:
                return maskIdCard(value);
            case EMAIL:
                return maskEmail(value);
            case BANK_CARD:
                return maskBankCard(value);
            default:
                return defaultMask(value);
        }
    }
    
    /**
     * 手机号脱敏
     * 规则: 保留前3位和后4位,中间用*代替
     */
    private static String maskPhone(String phone) {
        if (phone.length() < 7) {
            return defaultMask(phone);
        }
        return phone.substring(0, 3) + "****" + phone.substring(phone.length() - 4);
    }
    
    /**
     * 身份证号脱敏
     * 规则: 保留前6位和后4位,中间用*代替
     */
    private static String maskIdCard(String idCard) {
        if (idCard.length() < 10) {
            return defaultMask(idCard);
        }
        return idCard.substring(0, 6) + "********" + idCard.substring(idCard.length() - 4);
    }
    
    /**
     * 邮箱脱敏
     * 规则: 邮箱前缀仅显示第一个字符,后面用*代替,@及后面的地址显示完整
     */
    private static String maskEmail(String email) {
        if (!email.contains("@")) {
            return defaultMask(email);
        }
        int atIndex = email.indexOf('@');
        if (atIndex < 1) {
            return email;
        }
        return email.substring(0, 1) + "****" + email.substring(atIndex);
    }
    
    /**
     * 银行卡号脱敏
     * 规则: 保留前6位和后4位,中间用*代替
     */
    private static String maskBankCard(String bankCard) {
        if (bankCard.length() < 10) {
            return defaultMask(bankCard);
        }
        return bankCard.substring(0, 6) + "********" + bankCard.substring(bankCard.length() - 4);
    }

    /**
     * 默认脱敏规则
     * 规则: 保留首尾字符,中间用*代替
     */
    private static String defaultMask(String value) {
        if (value.length() <= 2) {
            return value;
        }
        return value.substring(0, 1) + "****" + value.substring(value.length() - 1);
    }
} 