package com.diit.ds.domain.resp;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.deser.std.StdDeserializer;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.IOException;
import java.util.List;
import java.util.Map;

/**
 * RAGFlow切片列表查询响应
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RagFlowChunkListResp {
    
    /**
     * 响应码
     */
    private Integer code;
    
    /**
     * 错误信息
     */
    @JsonProperty("message")
    private String message;
    
    /**
     * 切片数据
     * 使用自定义反序列化器处理data字段为布尔值的情况
     */
    @JsonProperty("data")
    @JsonDeserialize(using = ChunkDataDeserializer.class)
    private ChunkData data;
    
    /**
     * 自定义反序列化器，处理data字段为布尔值的情况
     */
    public static class ChunkDataDeserializer extends JsonDeserializer<ChunkData> {
        @Override
        public ChunkData deserialize(JsonParser p, DeserializationContext ctxt) throws IOException, JsonProcessingException {
            JsonNode node = p.getCodec().readTree(p);
            
            // 如果节点是布尔值，返回null
            if (node.isBoolean()) {
                return null;
            }
            
            // 否则正常解析对象
            return p.getCodec().treeToValue(node, ChunkData.class);
        }
    }
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ChunkData {
        
        /**
         * 切片列表
         */
        @JsonProperty("chunks")
        private List<ChunkInfo> chunks;
        
        /**
         * 文档信息
         */
        @JsonProperty("doc")
        private DocInfo doc;
        
        /**
         * 总数
         */
        @JsonProperty("total")
        private Integer total;
    }
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ChunkInfo {
        
        /**
         * 可用状态
         */
        @JsonProperty("available")
        private Boolean available;
        
        /**
         * 切片内容
         */
        @JsonProperty("content")
        private String content;

        /**
         * dataset ID
         */
        @JsonProperty("dataset_id")
        private String datasetId;
        
        /**
         * 文档名称关键词
         */
        @JsonProperty("docnm_kwd")
        private String docnmKwd;
        
        /**
         * 文档ID
         */
        @JsonProperty("document_id")
        private String documentId;
        
        /**
         * 切片ID
         */
        private String id;
        
        /**
         * 图片ID
         */
        @JsonProperty("image_id")
        private String imageId;
        
        /**
         * 重要关键词
         */
        @JsonProperty("important_keywords")
        private List<String> importantKeywords;
        
        /**
         * 位置信息，格式为[[页码, x1, y1, x2, y2], [...], ...]，或者空数组
         */
        private List<List<Integer>> positions;
        
        /**
         * 问题列表
         */
        @JsonProperty("questions")
        private List<String> questions;
    }
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class DocInfo {
        
        /**
         * 切片数量
         */
        @JsonProperty("chunk_count")
        private Integer chunkCount;
        
        /**
         * 切片方法
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
         * 数据集ID
         */
        @JsonProperty("dataset_id")
        private String datasetId;
        
        /**
         * 文档ID
         */
        private String id;
        
        /**
         * 文件存储位置
         */
        private String location;
        
        /**
         * 文件名称
         */
        private String name;
        
        /**
         * 元数据字段
         */
        @JsonProperty("meta_fields")
        private Map<String, Object> metaFields;
        
        /**
         * 解析配置
         */
        @JsonProperty("parser_config")
        private Map<String, Object> parserConfig;
        
        /**
         * 处理开始时间
         */
        @JsonProperty("process_begin_at")
        private String processBeginAt;
        
        /**
         * 处理持续时间
         */
        @JsonProperty("process_duation")
        private Double processDuation;
        
        /**
         * 进度
         */
        private Double progress;
        
        /**
         * 进度消息
         */
        @JsonProperty("progress_msg")
        private String progressMsg;
        
        /**
         * 运行状态
         */
        private String run;
        
        /**
         * 文件大小
         */
        private Long size;
        
        /**
         * 来源类型
         */
        @JsonProperty("source_type")
        private String sourceType;
        
        /**
         * 状态
         */
        private String status;
        
        /**
         * 缩略图
         */
        private String thumbnail;
        
        /**
         * 令牌数量
         */
        @JsonProperty("token_count")
        private Integer tokenCount;
        
        /**
         * 文件类型
         */
        private String type;
        
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
    }
} 