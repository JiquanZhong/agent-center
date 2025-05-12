package com.diit.ds.domain.entity;

import com.baomidou.dynamic.datasource.annotation.DS;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.util.Date;
import lombok.Data;

/**
 * 
 * @TableName knowledgebase
 */
@TableName(value ="knowledgebase")
@Data
@DS("ragflow")
public class Knowledgebase {
    /**
     * 
     */
    @TableId(value = "id")
    private String id;

    /**
     * 
     */
    @TableField(value = "create_time")
    private Long createTime;

    /**
     * 
     */
    @TableField(value = "create_date")
    private Date createDate;

    /**
     * 
     */
    @TableField(value = "update_time")
    private Long updateTime;

    /**
     * 
     */
    @TableField(value = "update_date")
    private Date updateDate;

    /**
     * 
     */
    @TableField(value = "avatar")
    private String avatar;

    /**
     * 
     */
    @TableField(value = "tenant_id")
    private String tenantId;

    /**
     * 
     */
    @TableField(value = "name")
    private String name;

    /**
     * 
     */
    @TableField(value = "language")
    private String language;

    /**
     * 
     */
    @TableField(value = "description")
    private String description;

    /**
     * 
     */
    @TableField(value = "embd_id")
    private String embdId;

    /**
     * 
     */
    @TableField(value = "permission")
    private String permission;

    /**
     * 
     */
    @TableField(value = "created_by")
    private String createdBy;

    /**
     * 
     */
    @TableField(value = "doc_num")
    private Integer docNum;

    /**
     * 
     */
    @TableField(value = "token_num")
    private Long tokenNum;

    /**
     * 
     */
    @TableField(value = "chunk_num")
    private Integer chunkNum;

    /**
     * 
     */
    @TableField(value = "similarity_threshold")
    private Double similarityThreshold;

    /**
     * 
     */
    @TableField(value = "vector_similarity_weight")
    private Double vectorSimilarityWeight;

    /**
     * 
     */
    @TableField(value = "parser_id")
    private String parserId;

    /**
     * 
     */
    @TableField(value = "parser_config")
    private String parserConfig;

    /**
     * 
     */
    @TableField(value = "pagerank")
    private Integer pagerank;

    /**
     * 
     */
    @TableField(value = "status")
    private String status;
}