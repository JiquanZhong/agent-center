package com.diit.system.controller;

import com.diit.common.rest.response.ResponseData;
import com.diit.system.bean.ChangePasswordRequest;
import com.diit.system.bean.JWTLoginResponse;
import com.diit.system.bean.LoginUser;
import com.diit.system.service.LoginService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import com.diit.system.basic.entity.User;

@RestController
@RequestMapping("/jwt")
public class LoginController {

    @Autowired
    private LoginService loginService;
    
    @PostMapping("/login")
    public ResponseData login(@RequestBody LoginUser loginUser){
//        String token = loginService.login(loginUser);
        JWTLoginResponse jwtLoginResponse = loginService.login(loginUser);
        return ResponseData.ok(jwtLoginResponse);
    }

    /**
     * 用户密码修改
     * @param request 包含用户账号、旧密码和新密码的请求体
     * @return 响应数据
     */
    @PostMapping("/changePassword")
    public ResponseData changePassword(@RequestBody ChangePasswordRequest request) {
        if (request.getUserId() == null || request.getUserId().trim().isEmpty()) {
            return ResponseData.error("用户账号不能为空");
        }
        return loginService.changePassword(request.getUserId(), request.getOldPassword(), request.getNewPassword());
    }

    /**
     * 用户注册
     * @param user 用户信息
     * @return 响应数据
     */
    @PostMapping("/userRegister")
    public ResponseData userRegister(@RequestBody User user) {
        return loginService.userRegister(user);
    }
}
