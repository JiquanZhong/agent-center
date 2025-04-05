package com.diit.ds.domain.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.util.Date;
import lombok.Data;

/**
 * RAG工作流文档片段表
 * @TableName workflow_chunk
 */
@TableName(value ="workflow_chunk")
@Data
public class WorkflowChunk {
    /**
     * 
     */
    @TableId(value = "id")
    private Long id;

    /**
     * 工作流运行ID
     */
    @TableField(value = "work_flow_run_id")
    private String workFlowRunId;

    /**
     * 片段ID
     */
    @TableField(value = "chunk_id")
    private String chunkId;

    /**
     * 
     */
    @TableField(value = "content")
    private String content;

    /**
     * 
     */
    @TableField(value = "content_ltks")
    private String contentLtks;

    /**
     * 
     */
    @TableField(value = "dataset_id")
    private String datasetId;

    /**
     * 
     */
    @TableField(value = "document_id")
    private String documentId;

    /**
     * 
     */
    @TableField(value = "document_keyword")
    private String documentKeyword;

    /**
     * 
     */
    @TableField(value = "highlight")
    private String highlight;

    /**
     * 
     */
    @TableField(value = "image_id")
    private String imageId;

    /**
     * 重要关键词(JSON数组)
     */
    @TableField(value = "important_keywords")
    private Object importantKeywords;

    /**
     * 
     */
    @TableField(value = "kb_id")
    private String kbId;

    /**
     * 
     */
    @TableField(value = "similarity")
    private Double similarity;

    /**
     * 
     */
    @TableField(value = "term_similarity")
    private Double termSimilarity;

    /**
     * 
     */
    @TableField(value = "vector_similarity")
    private Double vectorSimilarity;

    /**
     * 位置信息(JSON数组)
     */
    @TableField(value = "positions")
    private Object positions;

    /**
     * 
     */
    @TableField(value = "create_time")
    private Date createTime;

    /**
     *
     */
    @TableField(value = "index")
    private Integer index;
}