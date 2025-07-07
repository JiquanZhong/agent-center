package com.diit.system.bean;

import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

@Data
public class JWTLoginResponse {

    @ApiModelProperty(value = "user信息", notes = "登录成功后返回的用户信息", example = "{userId: '1', loginName: 'admin', userName: '超级管理员', userRoles: '管理员'}")
    private UserVO user;

    @ApiModelProperty(value = "登录令牌", notes = "用户登录成功后返回的令牌", example = "token123456")
    private String token;
}
