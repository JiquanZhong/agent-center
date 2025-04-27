package com.diit.ds.domain.req;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.util.List;

/**
 * RAGFlow创建数据集请求DTO
 * {
 *   "name": "ceshi",
 *   "avatar": "",
 *   "description": "ceshi",
 *   "embedding_model": "bge-large-zh-v1.5",
 *   "permission": "me",
 *   "chunk_method": "naive",
 *   "parser_config": {
 *     "auto_keywords": 0,
 *     "auto_questions": 0,
 *     "chunk_token_num": 512,
 *     "layout_recognize": "DeepDOC",
 *     "html4excel": false,
 *     "delimiter": "\n!?;。；！？",
 *     "task_page_size": 12,
 *     "raptor": {
 *       "use_raptor": false
 *     }
 *   }
 * }
 */
@Data
@JsonInclude(JsonInclude.Include.NON_NULL)  // 不序列化null值
public class RAGFlowDatasetCreateReq {
    /**
     * 数据集名称
     * 必填，必须符合以下要求：
     * - 允许字符包括：英文字母(a-z, A-Z)、数字(0-9)、下划线(_)
     * - 必须以英文字母或下划线开头
     * - 最大65,535个字符
     * - 大小写不敏感
     */
    @JsonProperty("name")
    private String name;

    /**
     * 数据集头像，Base64编码
     */
    @JsonProperty("avatar")
    private String avatar;

    /**
     * 数据集描述
     */
    @JsonProperty("description")
    private String description;

    /**
     * 嵌入模型名称，例如："bge-large-zh-v1.5"
     */
    @JsonProperty("embedding_model")
    private String embeddingModel = "bge-large-zh-v1.5";

    /**
     * 权限设置
     * 可选值：
     * - "me"：(默认) 只有自己可以管理数据集
     * - "team"：所有团队成员可以管理数据集
     */
    @JsonProperty("permission")
    private String permission = "team";

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
    private String chunkMethod = "naive";

    /**
     * 解析器配置
     * 配置项根据选择的分块方法不同而变化
     */
    @JsonProperty("parser_config")
    private ParserConfig parserConfig = new ParserConfig();

    /**
     * 解析器配置类，根据不同的分块方法有不同的属性
     */
    @Data
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class ParserConfig {
        /**
         * 是否进行自动关键词提取，默认为0
         */
        @JsonProperty("auto_keywords")
        private Integer autoKeywords = 5;

        /**
         * 是否进行问题提取，默认为0
         */
        @JsonProperty("auto_questions")
        private Integer autoQuestions = 2;

        /**
         * 分块token数量，默认为512
         */
        @JsonProperty("chunk_token_num")
        private Integer chunkTokenNum = 1024;

        /**
         * 是否进行布局识别，默认为DeepDOC
         */
        @JsonProperty("layout_recognize")
        private String layoutRecognize = "DeepDOC";

        /**
         * 是否将Excel文档转换为HTML格式，默认为false
         */
        @JsonProperty("html4excel")
        private Boolean html4excel = false;

        /**
         * 分隔符，默认为"\n!?;。；！？"
         */
        @JsonProperty("delimiter")
        private String delimiter = "\\n!?;。；！？";

        /**
         * PDF任务页面大小，默认为12
         */
        @JsonProperty("task_page_size")
        private Integer taskPageSize = 12;

        /**
         * Raptor特定设置
         */
        @JsonProperty("raptor")
        private RaptorConfig raptor;
        
        /**
         * GraphRAG特定设置
         */
        @JsonProperty("graphrag")
        private GraphRAGConfig graphrag;

        /**
         * 实体类型，仅当chunk_method为"knowledge_graph"时使用
         */
        @JsonProperty("entity_types")
        private List<String> entityTypes;

        /**
         * Raptor配置类
         */
        @Data
        public static class RaptorConfig {
            /**
             * 是否使用Raptor，默认为false
             */
            @JsonProperty("use_raptor")
            private Boolean useRaptor = false;
        }
        
        /**
         * GraphRAG配置类
         */
        @Data
        public static class GraphRAGConfig {
            /**
             * 是否使用GraphRAG，默认为false
             */
            @JsonProperty("use_graphrag")
            private Boolean useGraphRAG = false;
        }
    }
} 