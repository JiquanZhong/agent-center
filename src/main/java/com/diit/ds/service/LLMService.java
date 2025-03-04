package com.diit.ds.service;

import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.Map;

/**
 * 通用LLM服务接口
 * 定义与大语言模型交互的标准方法
 */
public interface LLMService {
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