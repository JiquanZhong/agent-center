package com.diit.ds.adapter;

import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.Map;

/**
 * LLM适配器接口
 * 用于适配不同的LLM服务提供商
 */
public interface LLMAdapter {
    /**
     * 处理阻塞模式的响应
     * 
     * @param requestBody 请求体
     * @return 响应结果
     */
    Map<String, Object> processBlockingResponse(Map<String, Object> requestBody);
    
    /**
     * 处理流式模式的响应
     * 
     * @param requestBody 请求体
     * @param emitter SSE发射器
     */
    void processStreamingResponse(Map<String, Object> requestBody, SseEmitter emitter);
} 