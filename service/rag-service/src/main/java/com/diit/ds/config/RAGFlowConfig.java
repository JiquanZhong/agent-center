package com.diit.ds.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * RAGFlow配置类
 */
@Data
@Configuration
@ConfigurationProperties(prefix = "ragflow.api")
public class RAGFlowConfig {
    
    /**
     * API基础URL
     */
    private String baseUrl;
    
    /**
     * API密钥
     */
    private String apiKey;

    /**
     * embeddings_id
     */
    private String embeddingsId;

    /**
     * rerank_id
     */
    private String rerankId;
} 