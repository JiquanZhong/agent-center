package com.diit.ds.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * H3C知识库配置类
 */
@Data
@Configuration
@ConfigurationProperties(prefix = "h3c.knowledge")
public class H3CKnowledgeConfig {
    /**
     * API基础URL
     */
    private String baseUrl = "https://ip:port";

    /**
     * 接口调用凭证
     */
    private String accessToken;

    /**
     * 默认知识库代码
     */
    private List<String> defaultKnowledgeCode = Collections.singletonList("knowledge1711518822958");

    /**
     * 问题类型
     */
    private String questionType = "0";
} 