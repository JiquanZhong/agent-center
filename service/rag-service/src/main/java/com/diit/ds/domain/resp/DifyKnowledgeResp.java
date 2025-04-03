package com.diit.ds.domain.resp;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.util.List;

/**
 * Dify外部知识库API响应类
 * 参考文档：https://docs.dify.ai/zh-hans/guides/knowledge-base/external-knowledge-api-documentation
 */
@Data
public class DifyKnowledgeResp {
    /**
     * 从知识库查询的记录列表
     */
    @JsonProperty("records")
    private List<Record> records;

    /**
     * 知识记录
     */
    @Data
    public static class Record {
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

        /**
         * 包含数据源中文档的元数据属性及其值
         */
        @JsonProperty("metadata")
        private Object metadata;
    }

    @Data
    public static class SimpleRecord {
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