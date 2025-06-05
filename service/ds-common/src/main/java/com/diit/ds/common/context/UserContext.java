package com.diit.ds.common.context;

import lombok.Data;
import lombok.experimental.UtilityClass;
import lombok.extern.slf4j.Slf4j;

import java.util.HashMap;
import java.util.Map;

/**
 * 用户上下文，用于在当前线程中存储用户信息
 */
@Slf4j
@UtilityClass
public class UserContext {

    /**
     * 使用ThreadLocal存储当前线程的用户上下文信息
     */
    private static final ThreadLocal<Map<String, Object>> CONTEXT = ThreadLocal.withInitial(HashMap::new);

    /**
     * 设置用户名
     * 
     * @param userName 用户名
     */
    public void setUserName(String userName) {
        CONTEXT.get().put("userName", userName);
        log.debug("用户上下文设置用户名: {}", userName);
    }

    /**
     * 获取用户名
     * 
     * @return 用户名
     */
    public String getUserName() {
        return (String) CONTEXT.get().get("userName");
    }

    /**
     * 设置用户ID
     * 
     * @param userId 用户ID
     */
    public void setUserId(String userId) {
        CONTEXT.get().put("userId", userId);
    }

    /**
     * 获取用户ID
     * 
     * @return 用户ID
     */
    public String getUserId() {
        return (String) CONTEXT.get().get("userId");
    }

    /**
     * 设置登录名
     * 
     * @param loginName 登录名
     */
    public void setLoginName(String loginName) {
        CONTEXT.get().put("loginName", loginName);
    }

    /**
     * 获取登录名
     * 
     * @return 登录名
     */
    public String getLoginName() {
        return (String) CONTEXT.get().get("loginName");
    }

    /**
     * 设置角色权限名
     *
     * @param userRoles 角色权限名，以,分割
     */
    public void setUserRoles(String userRoles) {
        CONTEXT.get().put("userRoles", userRoles);
    }

    /**
     * 获取角色权限名
     *
     * @return 角色权限名
     */
    public String getUserRoles() {
        return (String) CONTEXT.get().get("userRoles");
    }

    /**
     * 设置自定义属性
     * 
     * @param key 属性键
     * @param value 属性值
     */
    public void setAttribute(String key, Object value) {
        CONTEXT.get().put(key, value);
    }

    /**
     * 获取自定义属性
     * 
     * @param key 属性键
     * @return 属性值
     */
    public Object getAttribute(String key) {
        return CONTEXT.get().get(key);
    }

    /**
     * 获取所有上下文属性
     * 
     * @return 上下文属性映射
     */
    public Map<String, Object> getAttributes() {
        return new HashMap<>(CONTEXT.get());
    }

    /**
     * 清除当前线程的上下文信息
     * 应在请求处理完成后调用，防止内存泄漏
     */
    public void clear() {
        CONTEXT.remove();
        log.debug("清除用户上下文");
    }
} 