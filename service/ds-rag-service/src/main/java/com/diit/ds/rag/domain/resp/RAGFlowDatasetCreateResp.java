package com.diit.ds.rag.domain.resp;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

/**
 * RAGFlow创建数据集响应DTO
 * {
 *   "code": 0,
 *   "message": null,
 *   "data": {
 *     "avatar": "",
 *     "chunk_count": 0,
 *     "chunk_method": "naive",
 *     "create_date": "Sun, 30 Mar 2025 17:46:05 GMT",
 *     "create_time": 1743327965725,
 *     "created_by": "d6c121d80a0d11f084540242ac130006",
 *     "description": "ceshi",
 *     "document_count": 0,
 *     "embedding_model": "bge-large-zh-v1.5",
 *     "id": "ccbb4b820d4b11f0a3190242ac150006",
 *     "language": "English",
 *     "name": "ceshi",
 *     "parser_config": {
 *       "chunk_token_num": 512,
 *       "delimiter": "\n!?;。；！？",
 *       "html4excel": false,
 *       "layout_recognize": "DeepDOC",
 *       "raptor": {
 *         "use_raptor": false
 *       }
 *     },
 *     "permission": "me",
 *     "similarity_threshold": 0.2,
 *     "status": "1",
 *     "tenant_id": "d6c121d80a0d11f084540242ac130006",
 *     "token_num": 0,
 *     "update_date": "Sun, 30 Mar 2025 17:46:05 GMT",
 *     "update_time": 1743327965725,
 *     "vector_similarity_weight": 0.3
 *   }
 * }
 */
@Data
public class RAGFlowDatasetCreateResp {
    /**
     * 响应码，0表示成功
     */
    @JsonProperty("code")
    private int code;

    /**
     * 响应消息，成功时为null，失败时包含错误信息
     */
    @JsonProperty("message")
    private String message;

    /**
     * 响应数据，成功时包含数据集信息
     */
    @JsonProperty("data")
    private DatasetData data;

    /**
     * 数据集信息
     */
    @Data
    public static class DatasetData {
        /**
         * 数据集头像
         */
        @JsonProperty("avatar")
        private String avatar;

        /**
         * 分块数量
         */
        @JsonProperty("chunk_count")
        private Integer chunkCount;

        /**
         * 分块方法
         */
        @JsonProperty("chunk_method")
        private String chunkMethod;

        /**
         * 创建日期
         */
        @JsonProperty("create_date")
        private String createDate;

        /**
         * 创建时间戳
         */
        @JsonProperty("create_time")
        private Long createTime;

        /**
         * 创建者ID
         */
        @JsonProperty("created_by")
        private String createdBy;

        /**
         * 数据集描述
         */
        @JsonProperty("description")
        private String description;

        /**
         * 文档数量
         */
        @JsonProperty("document_count")
        private Integer documentCount;

        /**
         * 嵌入模型
         */
        @JsonProperty("embedding_model")
        private String embeddingModel;

        /**
         * 数据集ID
         */
        @JsonProperty("id")
        private String id;

        /**
         * 数据集语言
         */
        @JsonProperty("language")
        private String language;

        /**
         * 数据集名称
         */
        @JsonProperty("name")
        private String name;

        /**
         * 解析器配置
         */
        @JsonProperty("parser_config")
        private ParserConfig parserConfig;

        /**
         * 权限设置
         */
        @JsonProperty("permission")
        private String permission;

        /**
         * 相似度阈值
         */
        @JsonProperty("similarity_threshold")
        private Double similarityThreshold;

        /**
         * 状态
         */
        @JsonProperty("status")
        private String status;

        /**
         * 租户ID
         */
        @JsonProperty("tenant_id")
        private String tenantId;

        /**
         * token数量
         */
        @JsonProperty("token_num")
        private Integer tokenNum;

        /**
         * 更新日期
         */
        @JsonProperty("update_date")
        private String updateDate;

        /**
         * 更新时间戳
         */
        @JsonProperty("update_time")
        private Long updateTime;

        /**
         * 向量相似度权重
         */
        @JsonProperty("vector_similarity_weight")
        private Double vectorSimilarityWeight;

        /**
         * 解析器配置
         */
        @Data
        public static class ParserConfig {
            /**
             * 分块token数量
             */
            @JsonProperty("chunk_token_num")
            private Integer chunkTokenNum;

            /**
             * 分隔符
             */
            @JsonProperty("delimiter")
            private String delimiter;

            /**
             * 是否将Excel转为HTML
             */
            @JsonProperty("html4excel")
            private Boolean html4excel;

            /**
             * 是否进行布局识别
             */
            @JsonProperty("layout_recognize")
            private String layoutRecognize;

            /**
             * Raptor配置
             */
            @JsonProperty("raptor")
            private RaptorConfig raptor;

            /**
             * Raptor配置
             */
            @Data
            public static class RaptorConfig {
                /**
                 * 是否使用Raptor
                 */
                @JsonProperty("use_raptor")
                private Boolean useRaptor;
            }
        }
    }
} 