package com.diit.system.bean;

import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

@Data
public class LoginUser {

    @ApiModelProperty(value = "登录用户名",notes = "", example = "admin")
    private String loginName;

    @ApiModelProperty(value = "登录密码",notes = "", example = "admin")
    private String password;
}
