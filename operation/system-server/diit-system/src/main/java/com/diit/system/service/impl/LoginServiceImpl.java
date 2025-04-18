package com.diit.system.service.impl;

import com.alibaba.fastjson.JSON;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.diit.common.crypto.service.EncryptService;
import com.diit.system.basic.entity.Role;
import com.diit.system.basic.entity.User;
import com.diit.system.basic.entity.UserRole;
import com.diit.system.basic.mapper.RoleMapper;
import com.diit.system.basic.mapper.UserMapper;
import com.diit.system.basic.mapper.UserRoleMapper;
import com.diit.system.bean.LoginUser;
import com.diit.system.bean.UserVO;
import com.diit.system.service.LoginService;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.ObjectUtils;

import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
public class LoginServiceImpl implements LoginService {

    @Autowired
    private UserMapper userMapper;
    @Autowired
    private UserRoleMapper userRoleMapper;
    @Autowired
    private RoleMapper roleMapper;

    @Autowired
    private EncryptService encryptService;

    private static final long EXPIRATION_TIME = 86400000L * 30;

    @Value("${diit.crypto.jwt-secret-key}")
    private String SECRET_KEY;

    @Override
    public String login(LoginUser loginUser) {
        String loginName = loginUser.getLoginName();
        String password = loginUser.getPassword();
        if(ObjectUtils.isEmpty(loginName) || ObjectUtils.isEmpty(password)){
            throw new RuntimeException("用户或密码不正确");
        }
        User user = userMapper.selectOne(new QueryWrapper<User>().eq("login_name", loginName));
        List<String> roleIds = userRoleMapper.selectList(new LambdaQueryWrapper<UserRole>().eq(UserRole::getUserId, user.getId()))
                .stream()
                .map(userRole -> userRole.getRoleId())
                .collect(Collectors.toList());
        List<String> roles = roleMapper.selectList(new LambdaQueryWrapper<Role>().in(Role::getId, roleIds)).stream()
                .map(role -> role.getName())
                .collect(Collectors.toList());
        String roleName = String.join(",", roles);

        if(ObjectUtils.isEmpty(user)){
            throw new RuntimeException("用户或密码不正确");
        }
        if(!"1".equals(user.getStatus())){
            throw new RuntimeException("用户状态异常");
        }
        try {
            String loginPassword = encryptService.encrypt(password);
            if(loginPassword.equals(user.getPassword())){
                UserVO userVO = new UserVO();
                userVO.setUserId(user.getId());
                userVO.setUserName(user.getName());
                userVO.setLoginName(user.getLoginName());
                userVO.setUserRoles(roleName);
                return Jwts.builder()
                        .setSubject(JSON.toJSONString(userVO))
                        .setIssuedAt(new Date())
                        .setExpiration(new Date(System.currentTimeMillis() + EXPIRATION_TIME))
                        .signWith(SignatureAlgorithm.HS512, SECRET_KEY)
                        .compact();
            }
        }catch (Exception e) {
        }
        throw new RuntimeException("用户或密码不正确");
    }
}
