package com.diit.ds.domain.entity;

import java.util.Date;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

/**
 * 对话表
 * @TableName htyy_conversations
 */
@TableName(value ="htyy_conversations")
@Data
public class Conversations {
    /**
     * 对话ID
     */
    @TableId
    private String id;

    /**
     * 用户ID
     */
    private String userId;

    /**
     * 对话标题
     */
    private String title;

    /**
     * 创建时间
     */
    private Date createdAt;
}