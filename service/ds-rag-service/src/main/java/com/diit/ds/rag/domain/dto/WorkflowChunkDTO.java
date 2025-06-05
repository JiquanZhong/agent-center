package com.diit.ds.rag.domain.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

/**
 * RAG工作流文档片段表
 * @TableName workflow_chunk
 */
@Data
public class WorkflowChunkDTO {

    /**
     * 片段ID
     */
    @JsonProperty(value = "chunk_id")
    private String chunkId;

    /**
     * 
     */
    @JsonProperty(value = "content")
    private String content;

    /**
     * 
     */
    @JsonProperty(value = "content_ltks")
    private String contentLtks;

    /**
     * 
     */
    @JsonProperty(value = "dataset_id")
    private String datasetId;

    /**
     * 
     */
    @JsonProperty(value = "document_id")
    private String documentId;

    /**
     * 
     */
    @JsonProperty(value = "document_Keyword")
    private String documentKeyword;

    /**
     * 
     */
    @JsonProperty(value = "highlight")
    private String highlight;

    /**
     * 
     */
    @JsonProperty(value = "image_id")
    private String imageId;

    /**
     * 重要关键词(JSON数组)
     */
    @JsonProperty(value = "important_keywords")
    private Object importantKeywords;

    /**
     * 
     */
    @JsonProperty(value = "kb_id")
    private String kbId;

    /**
     * 
     */
    @JsonProperty(value = "similarity")
    private Double similarity;

    /**
     * 位置信息(二维数组)
     */
    @JsonProperty(value = "positions")
    private Object positions;

    /**
     *
     */
    @JsonProperty(value = "index")
    private Integer index;

    /**
     *
     */
    @JsonProperty(value = "preview_url")
    private String previewUrl;
}