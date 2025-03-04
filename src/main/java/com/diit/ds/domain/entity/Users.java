package com.diit.ds.domain.entity;

import java.util.Date;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

/**
 * 用户表
 * @TableName htyy_users
 */
@TableName(value ="htyy_users")
@Data
public class Users {
    /**
     * 用户ID
     */
    @TableId
    private String id;

    /**
     * 用户名
     */
    private String username;

    /**
     * 邮箱
     */
    private String email;

    /**
     * 密码哈希
     */
    private String passwordHash;

    /**
     * 头像URL
     */
    private String avatarUrl;

    /**
     * 用户角色（user/admin）
     */
    private String role;

    /**
     * 创建时间
     */
    private Date createdAt;
}