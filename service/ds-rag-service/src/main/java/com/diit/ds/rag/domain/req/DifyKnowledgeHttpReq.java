package com.diit.ds.rag.domain.req;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.util.List;

/**
 * Dify外部知识库API请求类
 * 参考文档：https://docs.dify.ai/zh-hans/guides/knowledge-base/external-knowledge-api-documentation
 */
@Data
@JsonInclude(JsonInclude.Include.NON_NULL)
public class DifyKnowledgeHttpReq {

    /**
     * work_flow_run_id
     */
    @JsonProperty("work_flow_run_id")
    private String workFlowRunId;

    @JsonProperty("select_all")
    private Boolean selectAll = true;

    @JsonProperty("kdb_ids")
    private List<String> kdbIds;

    /**
     * 用户的查询
     */
    @JsonProperty("query")
    private String query;

    /**
     * 知识检索参数
     */
    @JsonProperty("retrieval_setting")
    private RetrievalSetting retrievalSetting;

    /**
     * 知识检索参数
     */
    @Data
    public static class RetrievalSetting {
        /**
         * 检索结果的最大数量
         */
        @JsonProperty("top_k")
        private Integer topK = 5;

        /**
         * 结果与查询相关性的分数限制，范围：0~1
         */
        @JsonProperty("score_threshold")
        private Double scoreThreshold = 0.5;

        /**
         * 文本相似度和向量相似度的权重
         */
        @JsonProperty("vector_similarity_weight")
        private Double vectorSimilarityWeight = 0.5;

    }

} 