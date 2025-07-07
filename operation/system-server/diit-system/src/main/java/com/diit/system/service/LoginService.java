package com.diit.system.service;

import com.diit.common.rest.response.ResponseData;
import com.diit.system.bean.JWTLoginResponse;
import com.diit.system.bean.LoginUser;
import com.diit.system.basic.entity.User;

public interface LoginService {

    JWTLoginResponse login(LoginUser loginUser);
    
    ResponseData changePassword(String userId, String oldPassword, String newPassword);
    
    ResponseData userRegister(User user);
}
