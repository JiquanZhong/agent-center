package com.diit.system.controller;

import com.diit.common.rest.response.ResponseData;
import com.diit.system.bean.LoginUser;
import com.diit.system.service.LoginService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

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

    @PostMapping("/changePassword")
    public ResponseData changePassword(@RequestParam String userId, 
                                     @RequestParam String oldPassword, 
                                     @RequestParam String newPassword) {
        if (userId == null || userId.trim().isEmpty()) {
            return ResponseData.error("用户账号不能为空");
        }
        return loginService.changePassword(userId, oldPassword, newPassword);
    }
}
