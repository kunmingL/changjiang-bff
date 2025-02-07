package com.changjing.bff.dto;

import lombok.Data;

@Data
public class LoginUserInfo {
    private String userId;
    private String userName;
    private String userType;
    private String userRole;
    private String userOrgId;
    private String userOrgName;
    private String userOrgType;
}
