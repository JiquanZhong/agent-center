package com.diit.ds.rag.domain.req;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

/**
 * RAGFlow更新数据集请求DTO
 *{
 *   "name": "ceshi111",
 *   "embedding_model": "bge-large-zh-v1.5",
 *   "chunk_method": "qa"
 * }
 */
@Data
@JsonInclude(JsonInclude.Include.NON_NULL)  // 不序列化null值
public class RAGFlowDatasetUpdateReq {
    /**
     * 数据集名称
     */
    @JsonProperty("name")
    private String name;

    /**
     * 嵌入模型名称
     * 注意：更新嵌入模型前，确保chunk_count为0
     */
    @JsonProperty("embedding_model")
    private String embeddingModel;

    /**
     * 分块方法
     * 可选值：
     * - "naive"：通用 (默认)
     * - "manual"：手动
     * - "qa"：问答
     * - "table"：表格
     * - "paper"：论文
     * - "book"：书籍
     * - "laws"：法律
     * - "presentation"：演示文稿
     * - "picture"：图片
     * - "one"：单一
     * - "knowledge_graph"：知识图谱
     * - "email"：邮件
     */
    @JsonProperty("chunk_method")
    private String chunkMethod;
} 