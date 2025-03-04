package com.diit.ds.domain.entity;

import java.util.Date;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

/**
 * 模型管理表
 * @TableName htyy_models
 */
@TableName(value ="htyy_models")
@Data
public class Models {
    /**
     * 模型ID
     */
    @TableId
    private String id;

    /**
     * 模型名称
     */
    private String modelName;

    /**
     * API Key
     */
    private String apiKey;

    /**
     * 模型提供方（如OpenAI、Google等）
     */
    private String provider;

    /**
     * 最大Token数
     */
    private Integer maxTokens;

    /**
     * 温度参数，影响随机性
     */
    private Double temperature;

    /**
     * Top-p 采样参数
     */
    private Double topP;

    /**
     * 频率惩罚
     */
    private Double frequencyPenalty;

    /**
     * 存在惩罚
     */
    private Double presencePenalty;

    /**
     * 创建时间
     */
    private Date createdAt;

    /**
     * 更新时间
     */
    private Date updatedAt;

    /**
     * 添加模型的用户id
     */
    private String userId;
}