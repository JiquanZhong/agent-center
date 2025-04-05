package com.diit.ds.domain.dto;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.util.Date;

/**
 * RAG工作流文档聚合表
 * @TableName workflow_doc_agg
 */
@Data
public class WorkflowDocAggDTO {

    /**
     * 文档ID
     */
    @JsonProperty("document_id")
    private String docId;

    /**
     * 文档名称
     */
    @JsonProperty("document_name")
    private String docName;

    /**
     * 文档出现次数
     */
    @JsonProperty("count")
    private Integer count;

    /**
     * 预览链接
     */
    @JsonProperty("preview_url")
    private String previewUrl;

    /**
     * 文件类型
     */
    @JsonProperty("file_type")
    private String fileType;
}