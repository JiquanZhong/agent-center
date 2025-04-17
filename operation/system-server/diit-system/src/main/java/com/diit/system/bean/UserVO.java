package com.diit.system.bean;

import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

@Data
public class UserVO {

    @ApiModelProperty(value = "用户Id",notes = "", example = "-1")
    private String userId;

    @ApiModelProperty(value = "登录用户名",notes = "", example = "admin")
    private String loginName;

    @ApiModelProperty(value = "用户姓名",notes = "", example = "超级管理员")
    private String userName;

    @ApiModelProperty(value = "用户权限, 以‘,’号分割", example = "管理员")
    private String userRoles;
}
