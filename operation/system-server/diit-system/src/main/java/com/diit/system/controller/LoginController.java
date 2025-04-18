package com.diit.system.controller;

import com.diit.common.rest.response.ResponseData;
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
        String token = loginService.login(loginUser);
        return ResponseData.ok(token);
    }

    /**
     * 用户密码修改
     * @param userId 用户账号
     * @param oldPassword 旧密码
     * @param newPassword 新密码
     * @return 响应数据
     */
    @PostMapping("/changePassword")
    public ResponseData changePassword(@RequestParam String userId, 
                                     @RequestParam String oldPassword, 
                                     @RequestParam String newPassword) {
        if (userId == null || userId.trim().isEmpty()) {
            return ResponseData.error("用户账号不能为空");
        }
        return loginService.changePassword(userId, oldPassword, newPassword);
    }

    /**
     * 用户注册
     * @param user 用户信息
     * @return 响应数据
     */
    @PostMapping("/UserRegister")
    public ResponseData UserRegister(@RequestBody User user) {
        return loginService.UserRegister(user);
    }
}
