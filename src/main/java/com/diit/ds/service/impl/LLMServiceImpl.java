package com.diit.ds.service.impl;

import com.diit.ds.adapter.LLMAdapter;
import com.diit.ds.adapter.impl.DifyAdapter;
import com.diit.ds.adapter.impl.OllamaAdapter;
import com.diit.ds.service.LLMService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.Map;

/**
 * LLM服务实现类
 * 使用适配器模式选择合适的LLM服务提供商
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class LLMServiceImpl implements LLMService {

    private final DifyAdapter difyAdapter;
    private final OllamaAdapter ollamaAdapter;

    @Value("${llm.provider}")
    private String llmProvider;
    

    @Override
    public Map<String, Object> processBlockingResponse(Map<String, Object> requestBody) {
        // 根据配置选择适当的适配器
        LLMAdapter adapter = getAdapter();
        
        // 调用适配器处理请求
        return adapter.processBlockingResponse(requestBody);
    }

    @Override
    public void processStreamingResponse(Map<String, Object> requestBody, SseEmitter emitter) {
        // 根据配置选择适当的适配器
        LLMAdapter adapter = getAdapter();
        
        // 调用适配器处理请求
        adapter.processStreamingResponse(requestBody, emitter);
    }
    
    @Override
    public Map<String, Object> stopGenerating(String taskId, Map<String, Object> requestBody) {
        // 根据配置选择适当的适配器
        LLMAdapter adapter = getAdapter();
        
        // 调用适配器处理请求
        return adapter.stopGenerating(taskId, requestBody);
    }
    
    @Override
    public Map<String, Object> submitMessageFeedback(String messageId, Map<String, Object> requestBody) {
        // 根据配置选择适当的适配器
        LLMAdapter adapter = getAdapter();
        
        // 调用适配器处理请求
        return adapter.submitMessageFeedback(messageId, requestBody);
    }
    
    @Override
    public Map<String, Object> getMessageSuggestions(String messageId, String user) {
        // 根据配置选择适当的适配器
        LLMAdapter adapter = getAdapter();
        
        // 调用适配器处理请求
        return adapter.getMessageSuggestions(messageId, user);
    }
    
    @Override
    public Map<String, Object> getMessages(String user, String conversationId, Integer limit) {
        // 根据配置选择适当的适配器
        LLMAdapter adapter = getAdapter();
        
        // 调用适配器处理请求
        return adapter.getMessages(user, conversationId, limit);
    }
    
    @Override
    public Map<String, Object> getConversations(String user, String lastId, Integer limit) {
        // 根据配置选择适当的适配器
        LLMAdapter adapter = getAdapter();
        
        // 调用适配器处理请求
        return adapter.getConversations(user, lastId, limit);
    }
    
    @Override
    public Map<String, Object> deleteConversation(String conversationId, Map<String, Object> requestBody) {
        // 根据配置选择适当的适配器
        LLMAdapter adapter = getAdapter();
        
        // 调用适配器处理请求
        return adapter.deleteConversation(conversationId, requestBody);
    }
    
    @Override
    public Map<String, Object> renameConversation(String conversationId, Map<String, Object> requestBody) {
        // 根据配置选择适当的适配器
        LLMAdapter adapter = getAdapter();
        
        // 调用适配器处理请求
        return adapter.renameConversation(conversationId, requestBody);
    }
    
    @Override
    public Map<String, Object> audioToText(MultipartFile audioFile) {
        // 根据配置选择适当的适配器
        LLMAdapter adapter = getAdapter();
        
        // 调用适配器处理请求
        return adapter.audioToText(audioFile);
    }
    
    @Override
    public byte[] textToAudio(Map<String, Object> requestBody) {
        // 根据配置选择适当的适配器
        LLMAdapter adapter = getAdapter();
        
        // 调用适配器处理请求
        return adapter.textToAudio(requestBody);
    }
    
    @Override
    public Map<String, Object> getAppInfo() {
        // 根据配置选择适当的适配器
        LLMAdapter adapter = getAdapter();
        
        // 调用适配器处理请求
        return adapter.getAppInfo();
    }
    
    @Override
    public Map<String, Object> getParameters() {
        // 根据配置选择适当的适配器
        LLMAdapter adapter = getAdapter();
        
        // 调用适配器处理请求
        return adapter.getParameters();
    }
    
    @Override
    public Map<String, Object> getMeta() {
        // 根据配置选择适当的适配器
        LLMAdapter adapter = getAdapter();
        
        // 调用适配器处理请求
        return adapter.getMeta();
    }
    
    /**
     * 根据配置选择适当的适配器
     * 
     * @return 选择的适配器
     */
    private LLMAdapter getAdapter() {
        switch (llmProvider.toLowerCase()) {
            case "ollama":
                log.info("使用Ollama adapter");
                return ollamaAdapter;
            case "dify":
            default:
                log.info("使用Dify adapter");
                return difyAdapter;
        }
    }
} 