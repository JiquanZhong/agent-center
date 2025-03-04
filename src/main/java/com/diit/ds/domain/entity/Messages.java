package com.diit.ds.domain.entity;

import java.util.Date;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

/**
 * 消息表
 * @TableName htyy_messages
 */
@TableName(value ="htyy_messages")
@Data
public class Messages {
    /**
     * 消息ID
     */
    @TableId
    private String id;

    /**
     * 所属对话ID
     */
    private String conversationId;

    /**
     * 发送者类型（user/ai）
     */
    private String sender;

    /**
     * AI 名称（如果适用）
     */
    private String aiName;

    /**
     * 消息内容
     */
    private String content;

    /**
     * 消息索引
     */
    private Integer messageIndex;

    /**
     * 创建时间
     */
    private Date createdAt;
}