package com.diit.ds.domain.pojo.dto;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.extension.handlers.JacksonTypeHandler;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * 会话DTO类
 */
@Data
public class ConversationDTO {
    /**
     * 主键ID
     */
    private String id;
    
    /**
     * 应用ID
     */
    private String appId;
    
    /**
     * 应用模型配置ID
     *
     */
    private String appModelConfigId;
    
    /**
     * 模型提供商
     */
    private String modelProvider;
    
    /**
     * 覆盖模型配置
     */
    private String overrideModelConfigs;
    
    /**
     * 模型ID
     */
    private String modelId;
    
    /**
     * 模式
     */
    private String mode;
    
    /**
     * 名称
     */
    private String name;
    
    /**
     * 摘要
     */
    private String summary;
    
    /**
     * 输入参数
     */
    @TableField(typeHandler = JacksonTypeHandler.class)
    private Map<String, Object> inputs;
    
    /**
     * 介绍
     */
    private String introduction;
    
    /**
     * 系统指令
     */
    private String systemInstruction;
    
    /**
     * 系统指令令牌数
     */
    private Integer systemInstructionTokens;
    
    /**
     * 状态
     */
    private String status;
    
    /**
     * 来源
     */
    private String fromSource;
    
    /**
     * 来源终端用户ID
     */
    private String fromEndUserId;
    
    /**
     * 来源账户ID
     */
    private String fromAccountId;
    
    /**
     * 阅读时间
     */
    private LocalDateTime readAt;
    
    /**
     * 阅读账户ID
     */
    private String readAccountId;
    
    /**
     * 创建时间
     */
    private LocalDateTime createdAt;
    
    /**
     * 更新时间
     */
    private LocalDateTime updatedAt;
    
    /**
     * 是否删除
     */
    private Boolean isDeleted;
    
    /**
     * 调用来源
     */
    private String invokeFrom;
    
    /**
     * 对话数量
     */
    private Integer dialogueCount;

    /**
     * 会话用户id
     */
    private String fromEndUserSessionId;

    /**
     * 对话次数
     */
    private Integer messageCount;

    /**
     * 对话标题
     */
    private String systemName;
} 