package com.diit.ds.chat.service.impl;

import com.diit.ds.chat.adapter.LLMAdapter;
import com.diit.ds.chat.adapter.impl.DifyAdapter;
import com.diit.ds.chat.service.LLMService;
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

    @Value("${llm.provider}")
    private String llmProvider;
    

    @Override
    public Map<String, Object> processBlockingResponse(String agentId, Map<String, Object> requestBody) {
        // 根据配置选择适当的适配器
        LLMAdapter adapter = getAdapter();
        
        // 调用适配器处理请求
        return adapter.processBlockingResponse(agentId, requestBody);
    }

    @Override
    public void processStreamingResponse(String agentId, Map<String, Object> requestBody, SseEmitter emitter) {
        // 根据配置选择适当的适配器
        LLMAdapter adapter = getAdapter();
        
        // 调用适配器处理请求
        adapter.processStreamingResponse(agentId, requestBody, emitter);
    }
    
    @Override
    public Map<String, Object> stopGenerating(String agentId, String taskId, Map<String, Object> requestBody) {
        // 根据配置选择适当的适配器
        LLMAdapter adapter = getAdapter();
        
        // 调用适配器处理请求
        return adapter.stopGenerating(agentId, taskId, requestBody);
    }
    
    @Override
    public Map<String, Object> submitMessageFeedback(String agentId, String messageId, Map<String, Object> requestBody) {
        // 根据配置选择适当的适配器
        LLMAdapter adapter = getAdapter();
        
        // 调用适配器处理请求
        return adapter.submitMessageFeedback(agentId, messageId, requestBody);
    }
    
    @Override
    public Map<String, Object> getMessageSuggestions(String agentId, String messageId, String user) {
        // 根据配置选择适当的适配器
        LLMAdapter adapter = getAdapter();
        
        // 调用适配器处理请求
        return adapter.getMessageSuggestions(agentId, messageId, user);
    }
    
    @Override
    public Map<String, Object> getMessages(String agentId, String user, String conversationId, Integer limit) {
        // 根据配置选择适当的适配器
        LLMAdapter adapter = getAdapter();
        
        // 调用适配器处理请求
        return adapter.getMessages(agentId, user, conversationId, limit);
    }
    
    @Override
    public Map<String, Object> getConversations(String agentId, String user, String lastId, Integer limit, String keyword) {
        // 根据配置选择适当的适配器
        LLMAdapter adapter = getAdapter();
        
        // 调用适配器处理请求
        return adapter.getConversations(agentId, user, lastId, limit, keyword);
    }
    
    @Override
    public Map<String, Object> deleteConversation(String agentId, String conversationId, Map<String, Object> requestBody) {
        // 根据配置选择适当的适配器
        LLMAdapter adapter = getAdapter();
        
        // 调用适配器处理请求
        return adapter.deleteConversation(agentId, conversationId, requestBody);
    }
    
    @Override
    public Map<String, Object> renameConversation(String agentId, String conversationId, Map<String, Object> requestBody) {
        // 根据配置选择适当的适配器
        LLMAdapter adapter = getAdapter();
        
        // 调用适配器处理请求
        return adapter.renameConversation(agentId, conversationId, requestBody);
    }
    
    @Override
    public Map<String, Object> audioToText(String agentId, MultipartFile audioFile) {
        // 根据配置选择适当的适配器
        LLMAdapter adapter = getAdapter();
        
        // 调用适配器处理请求
        return adapter.audioToText(agentId, audioFile);
    }
    
    @Override
    public byte[] textToAudio(String agentId, Map<String, Object> requestBody) {
        // 根据配置选择适当的适配器
        LLMAdapter adapter = getAdapter();
        
        // 调用适配器处理请求
        return adapter.textToAudio(agentId, requestBody);
    }
    
    @Override
    public Map<String, Object> getAppInfo(String agentId) {
        // 根据配置选择适当的适配器
        LLMAdapter adapter = getAdapter();
        
        // 调用适配器处理请求
        return adapter.getAppInfo(agentId);
    }
    
    @Override
    public Map<String, Object> getParameters(String agentId) {
        // 根据配置选择适当的适配器
        LLMAdapter adapter = getAdapter();
        
        // 调用适配器处理请求
        return adapter.getParameters(agentId);
    }
    
    @Override
    public Map<String, Object> getMeta(String agentId) {
        return getAdapter().getMeta(agentId);
    }
    
    @Override
    public Map<String, Object> uploadFile(String agentId, MultipartFile file, String user) {
        return getAdapter().uploadFile(agentId, file, user);
    }
    
    /**
     * 根据配置选择适当的适配器
     * 
     * @return 选择的适配器
     */
    private LLMAdapter getAdapter() {
        switch (llmProvider.toLowerCase()) {
            case "dify":
            default:
                log.info("使用Dify adapter");
                return difyAdapter;
        }
    }
} 