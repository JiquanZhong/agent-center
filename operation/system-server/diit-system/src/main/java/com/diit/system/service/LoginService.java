package com.diit.system.service;

import com.diit.common.rest.response.ResponseData;
import com.diit.system.bean.LoginUser;

public interface LoginService {

    String login(LoginUser loginUser);
    
    ResponseData changePassword(String userId, String oldPassword, String newPassword);
}
