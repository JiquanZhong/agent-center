package com.diit.ds.chat.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Data
@Configuration
@ConfigurationProperties(prefix = "dify.api")
public class DifyConfig {

    private String baseUrl;

    private String chatAgentToken;
}
