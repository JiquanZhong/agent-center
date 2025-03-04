package com.diit.ds.domain.entity;

import java.util.Date;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

/**
 * 收藏表
 * @TableName htyy_favorites
 */
@TableName(value ="htyy_favorites")
@Data
public class Favorites {
    /**
     * 收藏ID
     */
    @TableId
    private String id;

    /**
     * 用户ID
     */
    private String userId;

    /**
     * 收藏的对话ID
     */
    private String conversationId;

    /**
     * 创建时间
     */
    private Date createdAt;
}