package com.diit.ds.domain.resp;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.util.List;

/**
 * Dify外部知识库API响应类
 * 参考文档：https://docs.dify.ai/zh-hans/guides/knowledge-base/external-knowledge-api-documentation
 */
@Data
public class DifyKnowledgeHttpResp {
    /**
     * 从知识库查询的记录列表
     */
    @JsonProperty("records")
    private List<SimpleRecord> records;

    @Data
    public static class SimpleRecord {

        /**
         * 此次查询中文本块的序列号
         */
        @JsonProperty("id")
        private Integer index;

        /**
         * 包含知识库中数据源的文本块
         */
        @JsonProperty("content")
        private String content;

        /**
         * 结果与查询的相关性分数，范围：0~1
         */
        @JsonProperty("score")
        private Double score;

        /**
         * 文档标题
         */
        @JsonProperty("title")
        private String title;
    }
} 