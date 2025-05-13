package com.diit.ds.config;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.Map;
import java.util.concurrent.TimeUnit;

@Configuration
public class TokenCacheConfig {

    @Value("${diit.security.token-cache.expire-minutes:5}")
    private int expireMinutes;

    @Value("${diit.security.token-cache.maximum-size:10000}")
    private int maximumSize;

    /**
     * SSO token验证结果缓存
     */
    @Bean(name = "ssoTokenCache")
    public Cache<String, Boolean> ssoTokenCache() {
        return Caffeine.newBuilder()
                .expireAfterWrite(expireMinutes, TimeUnit.MINUTES)
                .maximumSize(maximumSize)
                .build();
    }

    /**
     * JWT用户信息缓存
     */
    @Bean(name = "jwtUserInfoCache")
    public Cache<String, Map<String, String>> jwtUserInfoCache() {
        return Caffeine.newBuilder()
                .expireAfterWrite(expireMinutes, TimeUnit.MINUTES)
                .maximumSize(maximumSize)
                .build();
    }
} 