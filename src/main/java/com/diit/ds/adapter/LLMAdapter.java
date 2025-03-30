package com.diit.ds.adapter;

import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;
import org.springframework.web.multipart.MultipartFile;

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
    
    /**
     * 停止生成响应
     * 
     * @param taskId 任务ID
     * @param requestBody 请求体
     * @return 响应结果
     */
    Map<String, Object> stopGenerating(String taskId, Map<String, Object> requestBody);
    
    /**
     * 提交消息反馈
     * 
     * @param messageId 消息ID
     * @param requestBody 请求体
     * @return 响应结果
     */
    Map<String, Object> submitMessageFeedback(String messageId, Map<String, Object> requestBody);
    
    /**
     * 获取消息建议
     * 
     * @param messageId 消息ID
     * @param user 用户ID
     * @return 建议列表
     */
    Map<String, Object> getMessageSuggestions(String messageId, String user);
    
    /**
     * 获取消息列表
     * 
     * @param user 用户ID
     * @param conversationId 对话ID
     * @param limit 限制数量
     * @return 消息列表
     */
    Map<String, Object> getMessages(String user, String conversationId, Integer limit);
    
    /**
     * 获取对话列表
     * 
     * @param user 用户ID
     * @param lastId 最后一个对话ID
     * @param limit 限制数量
     * @param keyword 搜索关键字
     * @return 对话列表
     */
    Map<String, Object> getConversations(String user, String lastId, Integer limit, String keyword);

    /**
     * 获取对话列表
     *
     * @param user 用户ID
     * @param lastId 最后一个对话ID
     * @param limit 限制数量
     * @return 对话列表
     */
    Map<String, Object> getConversations(String user, String lastId, Integer limit);
    
    /**
     * 删除对话
     * 
     * @param conversationId 对话ID
     * @param requestBody 请求体
     * @return 响应结果
     */
    Map<String, Object> deleteConversation(String conversationId, Map<String, Object> requestBody);
    
    /**
     * 重命名对话
     * 
     * @param conversationId 对话ID
     * @param requestBody 请求体
     * @return 响应结果
     */
    Map<String, Object> renameConversation(String conversationId, Map<String, Object> requestBody);
    
    /**
     * 语音转文本
     * 
     * @param audioFile 音频文件
     * @return 转换结果
     */
    Map<String, Object> audioToText(MultipartFile audioFile);
    
    /**
     * 文本转语音
     * 
     * @param requestBody 请求体
     * @return 音频数据
     */
    byte[] textToAudio(Map<String, Object> requestBody);
    
    /**
     * 获取应用基本信息
     *
     * @return 应用信息
     */
    Map<String, Object> getAppInfo();
    
    /**
     * 获取应用参数信息
     *
     * @return 参数信息
     */
    Map<String, Object> getParameters();
    
    /**
     * 获取应用元数据
     *
     * @return 元数据信息
     */
    Map<String, Object> getMeta();
    
    /**
     * 上传文件
     * 
     * @param file 上传的文件
     * @param user 用户ID
     * @return 上传结果
     */
    Map<String, Object> uploadFile(MultipartFile file, String user);
} 