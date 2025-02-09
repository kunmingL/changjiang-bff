package com.changjiang.bff.dto;

import lombok.Data;

/**
 * 会话信息DTO
 * 主要职责：
 * 1. 封装用户会话的完整上下文信息
 * 2. 在分布式系统中传递用户状态
 * 3. 提供请求追踪和审计的基础数据
 * 
 * 使用场景：
 * - 跨服务调用时的用户上下文传递
 * - 分布式系统的用户状态同步
 * - 请求追踪和审计日志记录
 * 
 * 调用关系：
 * - 被DefaultController.assembleSessionInfo创建
 * - 被RrpcContext用于传递会话信息
 * - 被日志系统用于记录操作审计
 */
@Data
public class SessionInfo {
    /** 
     * 用户ID
     * 用户的唯一标识
     * 用于跨服务的用户身份识别
     */
    private String userId;
    
    /** 
     * 用户名称
     * 用户的显示名称
     * 用于日志记录和显示
     */
    private String userName;
    
    /** 
     * 请求IP地址
     * 记录用户请求的来源IP
     * 用于安全审计和访问控制
     */
    private String requestIpAddress;
    
    /** 用户类型 */
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