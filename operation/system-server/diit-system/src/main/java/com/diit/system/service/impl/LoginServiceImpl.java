package com.diit.system.service.impl;

import com.alibaba.fastjson.JSON;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.diit.common.crypto.service.EncryptService;
import com.diit.common.rest.response.ResponseData;
import com.diit.system.basic.entity.Role;
import com.diit.system.basic.entity.User;
import com.diit.system.basic.entity.UserRole;
import com.diit.system.basic.mapper.RoleMapper;
import com.diit.system.basic.mapper.UserMapper;
import com.diit.system.basic.mapper.UserRoleMapper;
import com.diit.system.basic.service.UserRoleService;
import com.diit.system.basic.service.UserService;
import com.diit.system.bean.JWTLoginResponse;
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
import com.diit.system.enums.*;

import java.util.Date;
import java.util.List;
import java.util.UUID;
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
    private UserService userService;
    @Autowired
    private UserRoleService userRoleService;

    @Autowired
    private EncryptService encryptService;

    private static final long EXPIRATION_TIME = 86400000L * 30;

    @Value("${diit.crypto.jwt-secret-key}")
    private String SECRET_KEY;

    @Override
    public JWTLoginResponse login(LoginUser loginUser) {
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
                String jwt = Jwts.builder()
                        .setSubject(JSON.toJSONString(userVO))
                        .setIssuedAt(new Date())
                        .setExpiration(new Date(System.currentTimeMillis() + EXPIRATION_TIME))
                        .signWith(SignatureAlgorithm.HS512, SECRET_KEY)
                        .compact();

                JWTLoginResponse jwtLoginResponse = new JWTLoginResponse();
                jwtLoginResponse.setUser(userVO);
                jwtLoginResponse.setToken(jwt);
                return jwtLoginResponse;
            }
        }catch (Exception e) {
            throw new RuntimeException("用户或密码不正确");
        }
        throw new RuntimeException("用户或密码不正确");
    }

    @Override
    public ResponseData changePassword(String userId, String oldPassword, String newPassword) {
        // 根据userId查询用户
        User user = userMapper.selectById(userId);
        if (user == null) {
            return ResponseData.error("用户不存在");
        }

        // 验证旧密码
        try {
            if (!encryptService.match(oldPassword, user.getPassword())) {
                return ResponseData.error("旧密码不正确");
            }
        } catch (Exception e) {
            log.error("密码验证失败", e);
            return ResponseData.error("密码验证失败");
        }

        // 加密新密码
        try {
            String encryptedNewPassword = encryptService.encrypt(newPassword);
            user.setPassword(encryptedNewPassword);
            user.setPwdModifyTime(new Date());
            userMapper.updateById(user);
            return ResponseData.ok("密码修改成功");
        } catch (Exception e) {
            log.error("密码修改失败", e);
            return ResponseData.error("密码修改失败");
        }
    }

    @Override
    public ResponseData userRegister(User user) {
        try {
            // 检查用户名是否已存在
            User existingUser = userMapper.selectOne(new QueryWrapper<User>()
                    .eq("login_name", user.getLoginName()));
            if (existingUser != null) {
                return ResponseData.error("用户名已存在");
            }
            // 生成UUID作为用户ID，并移除'-'字符
            String userId = UUID.randomUUID().toString().replace("-", "");
            user.setId(userId);
            // 设置创建时间为当前时间
            user.setCreateTime(new Date());
            // 设置默认状态为启用
            user.setStatus("1");
            // 保存用户信息,判断是否有自定义密码
            if (ObjectUtils.isEmpty(user.getPassword())){
                userService.insert_new(user);
            }else{
                user.setPassword(encryptService.encrypt(user.getPassword()));
                user.setPwdModifyTime(new Date());
                try {
                    user.setEmailPassword(userService.getEncryptMailPassword(user.getEmailPassword()));
                } catch (Exception var4) {
                    Exception e = var4;
                    this.log.error("邮箱密码加密失败", e);
                    throw new RuntimeException("邮箱密码加密失败");
                }
                user.setDel(0);
                userMapper.insert(user);
            }
            // 给用户分配默认角色
            Role role = roleMapper.selectOne(new QueryWrapper<Role>().eq("name", RoleEnum.NORMAL.getName()));
            userRoleService.addUserToRole(user.getId(), role.getId());
            return ResponseData.ok("用户注册成功");
        } catch (Exception e) {
            log.error("用户注册失败", e);
            return ResponseData.error("用户注册失败：" + e.getMessage());
        }
    }
}
