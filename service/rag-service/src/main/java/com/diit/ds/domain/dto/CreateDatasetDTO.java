package com.diit.ds.domain.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data
public class CreateDatasetDTO {
    private String id;
    private String avatar;
    
    @JsonProperty("chunk_count")
    private Integer chunkCount;
    
    @JsonProperty("chunk_method")
    private String chunkMethod;
    
    @JsonProperty("create_date")
    private String createDate;
    
    @JsonProperty("create_time")
    private Long createTime;
    
    @JsonProperty("created_by")
    private String createdBy;
    
    private String description;
    
    @JsonProperty("document_count")
    private Integer documentCount;
    
    @JsonProperty("embedding_model")
    private String embeddingModel;
    
    private String language;
    private String name;
    
    @JsonProperty("parser_config")
    private ParserConfig parserConfig;
    
    private String permission;
    
    @JsonProperty("similarity_threshold")
    private Double similarityThreshold;
    
    private String status;
    
    @JsonProperty("tenant_id")
    private String tenantId;
    
    @JsonProperty("token_num")
    private Integer tokenNum;
    
    @JsonProperty("update_date")
    private String updateDate;
    
    @JsonProperty("update_time")
    private Long updateTime;
    
    @JsonProperty("vector_similarity_weight")
    private Double vectorSimilarityWeight;

    @Data
    public static class ParserConfig {
        @JsonProperty("chunk_token_num")
        private Integer chunkTokenNum;
        
        private String delimiter;
        
        @JsonProperty("html4excel")
        private Boolean html4excel;
        
        @JsonProperty("layout_recognize")
        private Boolean layoutRecognize;
        
        private RaptorConfig raptor;
    }

    @Data
    public static class RaptorConfig {
        @JsonProperty("user_raptor")
        private Boolean userRaptor;
    }
} 