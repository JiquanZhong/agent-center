package com.diit.ds.service;

import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;
import org.springframework.web.multipart.MultipartFile;

import java.util.Map;

/**
 * 通用LLM服务接口
 * 定义与大语言模型交互的标准方法
 */
public interface LLMService {
    /**
     * 处理阻塞模式的响应
     * 
     * @param agentId 智能体ID
     * @param requestBody 请求体
     * @return 响应结果
     */
    Map<String, Object> processBlockingResponse(String agentId, Map<String, Object> requestBody);
    
    /**
     * 处理流式模式的响应
     * 
     * @param agentId 智能体ID
     * @param requestBody 请求体
     * @param emitter SSE发射器
     */
    void processStreamingResponse(String agentId, Map<String, Object> requestBody, SseEmitter emitter);
    
    /**
     * 停止生成响应
     * 
     * @param agentId 智能体ID
     * @param taskId 任务ID
     * @param requestBody 请求体
     * @return 响应结果
     */
    Map<String, Object> stopGenerating(String agentId, String taskId, Map<String, Object> requestBody);
    
    /**
     * 提交消息反馈
     * 
     * @param agentId 智能体ID
     * @param messageId 消息ID
     * @param requestBody 请求体
     * @return 响应结果
     */
    Map<String, Object> submitMessageFeedback(String agentId, String messageId, Map<String, Object> requestBody);
    
    /**
     * 获取消息建议
     * 
     * @param agentId 智能体ID
     * @param messageId 消息ID
     * @param user 用户ID
     * @return 建议列表
     */
    Map<String, Object> getMessageSuggestions(String agentId, String messageId, String user);
    
    /**
     * 获取消息列表
     * 
     * @param agentId 智能体ID
     * @param user 用户ID
     * @param conversationId 对话ID
     * @param limit 限制数量
     * @return 消息列表
     */
    Map<String, Object> getMessages(String agentId, String user, String conversationId, Integer limit);

    /**
     * 获取对话列表
     *
     * @param agentId 智能体ID
     * @param user 用户ID
     * @param lastId 最后一个对话ID
     * @param limit 限制数量
     * @param keyword 搜索关键字
     * @return 对话列表
     */
    Map<String, Object> getConversations(String agentId, String user, String lastId, Integer limit, String keyword);
    
    /**
     * 删除对话
     * 
     * @param agentId 智能体ID
     * @param conversationId 对话ID
     * @param requestBody 请求体
     * @return 响应结果
     */
    Map<String, Object> deleteConversation(String agentId, String conversationId, Map<String, Object> requestBody);
    
    /**
     * 重命名对话
     * 
     * @param agentId 智能体ID
     * @param conversationId 对话ID
     * @param requestBody 请求体
     * @return 响应结果
     */
    Map<String, Object> renameConversation(String agentId, String conversationId, Map<String, Object> requestBody);
    
    /**
     * 语音转文本
     * 
     * @param agentId 智能体ID
     * @param audioFile 音频文件
     * @return 转换结果
     */
    Map<String, Object> audioToText(String agentId, MultipartFile audioFile);
    
    /**
     * 文本转语音
     * 
     * @param agentId 智能体ID
     * @param requestBody 请求体
     * @return 音频数据
     */
    byte[] textToAudio(String agentId, Map<String, Object> requestBody);

    /**
     * 获取应用基本信息
     *
     * @param agentId 智能体ID
     * @return 应用信息
     */
    Map<String, Object> getAppInfo(String agentId);
    
    /**
     * 获取应用参数信息
     *
     * @param agentId 智能体ID
     * @return 参数信息
     */
    Map<String, Object> getParameters(String agentId);
    
    /**
     * 获取应用元数据
     *
     * @param agentId 智能体ID
     * @return 元数据信息
     */
    Map<String, Object> getMeta(String agentId);
    
    /**
     * 上传文件
     * 
     * @param agentId 智能体ID
     * @param file 上传的文件
     * @param user 用户ID
     * @return 上传结果
     */
    Map<String, Object> uploadFile(String agentId, MultipartFile file, String user);
}