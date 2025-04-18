package com.diit.system.bean;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;

@ApiModel("修改密码请求参数")
public class ChangePasswordRequest {
    
    @ApiModelProperty(value = "用户ID", required = true)
    private String userId;
    
    @ApiModelProperty(value = "旧密码", required = true)
    private String oldPassword;
    
    @ApiModelProperty(value = "新密码", required = true)
    private String newPassword;

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public String getOldPassword() {
        return oldPassword;
    }

    public void setOldPassword(String oldPassword) {
        this.oldPassword = oldPassword;
    }

    public String getNewPassword() {
        return newPassword;
    }

    public void setNewPassword(String newPassword) {
        this.newPassword = newPassword;
    }
} 