package com.diit.ds.domain.entity;

import com.baomidou.dynamic.datasource.annotation.DS;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.util.Date;
import lombok.Data;

/**
 * 
 * @TableName document
 */
@TableName(value ="document")
@Data
@DS("ragflow")
public class Document {
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
    @TableField(value = "thumbnail")
    private String thumbnail;

    /**
     * 
     */
    @TableField(value = "kb_id")
    private String kbId;

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
    @TableField(value = "source_type")
    private String sourceType;

    /**
     * 
     */
    @TableField(value = "type")
    private String type;

    /**
     * 
     */
    @TableField(value = "created_by")
    private String createdBy;

    /**
     * 
     */
    @TableField(value = "name")
    private String name;

    /**
     * 
     */
    @TableField(value = "location")
    private String location;

    /**
     * 
     */
    @TableField(value = "size")
    private Integer size;

    /**
     * 
     */
    @TableField(value = "token_num")
    private Integer tokenNum;

    /**
     * 
     */
    @TableField(value = "chunk_num")
    private Integer chunkNum;

    /**
     * 
     */
    @TableField(value = "progress")
    private Double progress;

    /**
     * 
     */
    @TableField(value = "progress_msg")
    private String progressMsg;

    /**
     * 
     */
    @TableField(value = "process_begin_at")
    private Date processBeginAt;

    /**
     * 
     */
    @TableField(value = "process_duation")
    private Double processDuation;

    /**
     * 
     */
    @TableField(value = "meta_fields")
    private String metaFields;

    /**
     * 
     */
    @TableField(value = "run")
    private String run;

    /**
     * 
     */
    @TableField(value = "status")
    private String status;
}