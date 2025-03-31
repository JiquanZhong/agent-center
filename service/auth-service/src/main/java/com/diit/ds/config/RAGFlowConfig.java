package com.diit.ds.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Data
@Configuration
@ConfigurationProperties(prefix = "rag-flow.api")
public class RAGFlowConfig {

    /**
     * RAGFlow API的基础URL，例如：http://address
     */
    private String baseUrl;

    /**
     * RAGFlow API的认证密钥
     */
    private String apiKey;
}
