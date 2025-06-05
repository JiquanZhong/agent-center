package com.diit.ds.domain.pojo.entity.dify;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.util.Date;

/**
 * 
 * @TableName messages
 */
@TableName(value ="messages")
@Data
public class Messages {
    /**
     * 
     */
    @TableId(value = "id")
    private Object id;

    /**
     * 
     */
    @TableField(value = "app_id")
    private Object appId;

    /**
     * 
     */
    @TableField(value = "model_provider")
    private String modelProvider;

    /**
     * 
     */
    @TableField(value = "model_id")
    private String modelId;

    /**
     * 
     */
    @TableField(value = "override_model_configs")
    private String overrideModelConfigs;

    /**
     * 
     */
    @TableField(value = "conversation_id")
    private Object conversationId;

    /**
     * 
     */
    @TableField(value = "inputs")
    private Object inputs;

    /**
     * 
     */
    @TableField(value = "query")
    private String query;

    /**
     * 
     */
    @TableField(value = "message")
    private Object message;

    /**
     * 
     */
    @TableField(value = "message_tokens")
    private Integer messageTokens;

    /**
     * 
     */
    @TableField(value = "message_unit_price")
    private BigDecimal messageUnitPrice;

    /**
     * 
     */
    @TableField(value = "answer")
    private String answer;

    /**
     * 
     */
    @TableField(value = "answer_tokens")
    private Integer answerTokens;

    /**
     * 
     */
    @TableField(value = "answer_unit_price")
    private BigDecimal answerUnitPrice;

    /**
     * 
     */
    @TableField(value = "provider_response_latency")
    private Double providerResponseLatency;

    /**
     * 
     */
    @TableField(value = "total_price")
    private BigDecimal totalPrice;

    /**
     * 
     */
    @TableField(value = "currency")
    private String currency;

    /**
     * 
     */
    @TableField(value = "from_source")
    private String fromSource;

    /**
     * 
     */
    @TableField(value = "from_end_user_id")
    private Object fromEndUserId;

    /**
     * 
     */
    @TableField(value = "from_account_id")
    private Object fromAccountId;

    /**
     * 
     */
    @TableField(value = "created_at")
    private Date createdAt;

    /**
     * 
     */
    @TableField(value = "updated_at")
    private Date updatedAt;

    /**
     * 
     */
    @TableField(value = "agent_based")
    private Boolean agentBased;

    /**
     * 
     */
    @TableField(value = "message_price_unit")
    private BigDecimal messagePriceUnit;

    /**
     * 
     */
    @TableField(value = "answer_price_unit")
    private BigDecimal answerPriceUnit;

    /**
     * 
     */
    @TableField(value = "workflow_run_id")
    private Object workflowRunId;

    /**
     * 
     */
    @TableField(value = "status")
    private String status;

    /**
     * 
     */
    @TableField(value = "error")
    private String error;

    /**
     * 
     */
    @TableField(value = "message_metadata")
    private String messageMetadata;

    /**
     * 
     */
    @TableField(value = "invoke_from")
    private String invokeFrom;

    /**
     * 
     */
    @TableField(value = "parent_message_id")
    private Object parentMessageId;
}