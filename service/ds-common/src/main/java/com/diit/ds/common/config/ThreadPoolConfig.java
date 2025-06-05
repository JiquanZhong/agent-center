package com.diit.ds.common.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.stereotype.Component;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

@Configuration
public class ThreadPoolConfig {

    @Bean
    public ExecutorService executorService(ThreadPoolProperties properties) {
        return new ThreadPoolExecutor(
                properties.getCorePoolSize(),
                properties.getMaxPoolSize(),
                properties.getKeepAliveTime(),
                TimeUnit.SECONDS,
                new LinkedBlockingQueue<>(properties.getQueueCapacity()),
                new ThreadPoolExecutor.CallerRunsPolicy()
        );
    }

    @Data
    @Component
    @ConfigurationProperties(prefix = "thread-pool")
    public static class ThreadPoolProperties {
        /**
         * 核心线程数
         */
        private int corePoolSize = 5;
        
        /**
         * 最大线程数
         */
        private int maxPoolSize = 10;
        
        /**
         * 线程空闲时间（秒）
         */
        private long keepAliveTime = 60;
        
        /**
         * 队列容量
         */
        private int queueCapacity = 100;
    }
} 