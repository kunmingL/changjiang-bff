package com.changjiang.bff.dto;

import lombok.Data;

/**
 * 登录用户信息DTO
 * 主要职责：
 * 1. 封装用户的登录信息和权限数据
 * 2. 在系统各层间传递用户上下文
 * 3. 提供用户身份和权限验证的基础数据
 * 
 * 使用场景：
 * - 用户登录后的身份信息存储
 * - 权限验证和访问控制
 * - 业务操作的用户上下文传递
 * 
 * 调用关系：
 * - 被SessionUtils创建和管理
 * - 被DefaultController用于构建SessionInfo
 * - 被权限拦截器用于进行权限校验
 */
@Data
public class LoginUserInfo {
    /** 
     * 用户ID
     * 用户的唯一标识
     * 用于用户身份识别和关联
     */
    private String userId;
    
    /** 
     * 用户名称
     * 用户的显示名称
     * 用于日志记录和显示
     */
    private String userName;
    
    /** 
     * 用户类型
     * 标识用户的分类
     * 用于区分不同类型的用户
     */
    private String userType;
    
    /** 用户角色 */
    private String userRole;
    
    /** 用户组织ID */
    private String userOrgId;
    
    /** 用户组织名称 */
    private String userOrgName;
    
    /** 用户组织类型 */
    private String userOrgType;
}
