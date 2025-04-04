package com.diit.ds.web;

import com.diit.ds.service.LLMService;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.Map;

@Slf4j
@Tag(name = "智能体的Controller，接口为Dify的调用格式")
@RestController
@RequestMapping("/v1")
@RequiredArgsConstructor
public class DifyChatAPIController {

    private final LLMService llmService;
    
    /**
     * 处理聊天消息请求
     * 根据response_mode参数决定返回阻塞模式或流式模式的响应
     */
    @PostMapping("/chat-messages")
    public Object chatMessages(
            @RequestBody Map<String, Object> requestBody) {
        
        String responseMode = (String) requestBody.getOrDefault("response_mode", "blocking");
        
        if ("streaming".equals(responseMode)) {
            // 流式响应模式
            SseEmitter emitter = new SseEmitter(-1L); // 无超时
            llmService.processStreamingResponse(requestBody, emitter);
            return emitter;
        } else {
            // 阻塞响应模式
            return llmService.processBlockingResponse(requestBody);
        }
    }
    
    /**
     * 停止生成响应
     * 用于中断正在进行的流式响应
     */
    @PostMapping("/chat-messages/{taskId}/stop")
    public Map<String, Object> stopChatMessages(
            @PathVariable String taskId,
            @RequestBody Map<String, Object> requestBody) {
        log.info("停止生成响应，taskId: {}", taskId);
        return llmService.stopGenerating(taskId, requestBody);
    }

    /**
     * 提交消息反馈
     * 用于对消息进行评价（如点赞、点踩等）
     */
    @PostMapping("/messages/{messageId}/feedbacks")
    public Map<String, Object> submitMessageFeedback(
            @PathVariable String messageId,
            @RequestBody Map<String, Object> requestBody) {
        log.info("提交消息反馈，messageId: {}, feedback: {}", messageId, requestBody);
        return llmService.submitMessageFeedback(messageId, requestBody);
    }
    
    /**
     * 获取消息建议
     * 用于获取特定消息的建议回复
     */
    @GetMapping("/messages/{messageId}/suggested")
    public Map<String, Object> getMessageSuggestions(
            @PathVariable String messageId,
            @RequestParam String user) {
        log.info("获取消息建议，messageId: {}, user: {}", messageId, user);
        return llmService.getMessageSuggestions(messageId, user);
    }
    
    /**
     * 获取消息列表
     * 用于获取用户的消息历史记录
     */
    @GetMapping("/messages")
    public Map<String, Object> getMessages(
            @RequestParam String user,
            @RequestParam(required = false) String conversation_id,
            @RequestParam(required = false, defaultValue = "20") Integer limit) {
        log.info("获取消息列表，user: {}, conversation_id: {}, limit: {}", user, conversation_id, limit);
        return llmService.getMessages(user, conversation_id, limit);
    }
    
    /**
     * 获取对话列表
     * 用于获取用户的对话历史记录
     */
    @GetMapping("/conversations")
    public Map<String, Object> getConversations(
            @RequestParam String user,
            @RequestParam(required = false) String last_id,
            @RequestParam(required = false, defaultValue = "20") Integer limit,
            @RequestParam(required = false) String keyWord) {
        log.info("获取对话列表，user: {}, last_id: {}, limit: {}", user, last_id, limit);
        return llmService.getConversations(user, last_id, limit, keyWord);
    }
    
    /**
     * 删除对话
     * 用于删除指定的对话
     */
    @DeleteMapping("/conversations/{conversationId}")
    public Map<String, Object> deleteConversation(
            @PathVariable String conversationId,
            @RequestBody Map<String, Object> requestBody) {
        log.info("删除对话，conversationId: {}, requestBody: {}", conversationId, requestBody);
        return llmService.deleteConversation(conversationId, requestBody);
    }
    
    /**
     * 重命名对话
     * 用于重命名指定的对话
     */
    @PostMapping("/conversations/{conversationId}/name")
    public Map<String, Object> renameConversation(
            @PathVariable String conversationId,
            @RequestBody Map<String, Object> requestBody) {
        log.info("重命名对话，conversationId: {}, requestBody: {}", conversationId, requestBody);
        return llmService.renameConversation(conversationId, requestBody);
    }
    
    /**
     * 语音转文本
     * 用于将语音转换为文本
     */
    @PostMapping("/audio-to-text")
    public Map<String, Object> audioToText(
            @RequestParam("file") MultipartFile audioFile) {
        log.info("语音转文本，文件名: {}, 文件大小: {}", audioFile.getOriginalFilename(), audioFile.getSize());
        return llmService.audioToText(audioFile);
    }
    
    /**
     * 文本转语音
     * 用于将文本转换为语音
     */
    @PostMapping("/text-to-audio")
    public ResponseEntity<byte[]> textToAudio(
            @RequestBody Map<String, Object> requestBody) {
        log.info("文本转语音，requestBody: {}", requestBody);
        byte[] audioData = llmService.textToAudio(requestBody);
        
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.parseMediaType("audio/wav"));
        
        return ResponseEntity.ok()
                .headers(headers)
                .body(audioData);
    }

    /**
     * 获取应用基本信息
     */
    @GetMapping("/info")
    public Map<String, Object> getAppInfo() {
        log.info("获取应用基本信息");
        return llmService.getAppInfo();
    }
    
    /**
     * 获取应用参数信息
     */
    @GetMapping("/parameters")
    public Map<String, Object> getParameters() {
        log.info("获取应用参数信息");
        return llmService.getParameters();
    }
    
    /**
     * 获取应用元数据
     */
    @GetMapping("/meta")
    public Map<String, Object> getMeta() {
        log.info("获取应用元数据");
        return llmService.getMeta();
    }

    /**
     * 上传文件
     * 用于上传图片等文件到系统
     */
    @PostMapping("/files/upload")
    public Map<String, Object> uploadFile(
            @RequestParam("file") MultipartFile file,
            @RequestParam("user") String user) {
        log.info("上传文件，文件名: {}, 文件大小: {}, 用户: {}", file.getOriginalFilename(), file.getSize(), user);
        return llmService.uploadFile(file, user);
    }

}
