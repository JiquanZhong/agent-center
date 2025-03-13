package com.diit.ds.domain.req;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

/**
 * Dify外部知识库API请求类
 * 参考文档：https://docs.dify.ai/zh-hans/guides/knowledge-base/external-knowledge-api-documentation
 */
@Data
@JsonInclude(JsonInclude.Include.NON_NULL)
public class DifyKnowledgeReq {
    /**
     * 知识库唯一ID
     */
    @JsonProperty("knowledge_id")
    private String knowledgeId;

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
    }
} 