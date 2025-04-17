package com.diit.system.controller;

import com.diit.common.rest.response.ResponseData;
import com.diit.system.bean.LoginUser;
import com.diit.system.service.LoginService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

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

}
