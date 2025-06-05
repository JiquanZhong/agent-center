package com.diit.ds.web;

import com.diit.ds.common.context.UserContext;
import com.diit.ds.chat.service.LLMService;
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
            @RequestBody Map<String, Object> requestBody,
            @RequestParam(required = false) String agentId) {
        
        String responseMode = (String) requestBody.getOrDefault("response_mode", "blocking");

        handleDefaultUserInfo(requestBody);
        
        if ("streaming".equals(responseMode)) {
            // 流式响应模式
            SseEmitter emitter = new SseEmitter(-1L); // 无超时
            llmService.processStreamingResponse(agentId, requestBody, emitter);
            return emitter;
        } else {
            // 阻塞响应模式
            return llmService.processBlockingResponse(agentId, requestBody);
        }
    }

    private void handleDefaultUserInfo(Map<String, Object> requestBody) {
        if (!requestBody.containsKey("user") || requestBody.get("user") == null || requestBody.get("user").toString().isEmpty()) {
            // 如果没有指定用户，则使用默认用户
            requestBody.put("user", UserContext.getUserId());
        }
    }

    /**
     * 停止生成响应
     * 用于中断正在进行的流式响应
     */
    @PostMapping("/chat-messages/{taskId}/stop")
    public Map<String, Object> stopChatMessages(
            @PathVariable String taskId,
            @RequestBody Map<String, Object> requestBody,
            @RequestParam(required = false) String agentId) {
        log.info("停止生成响应，taskId: {}, agentId: {}", taskId, agentId);
        return llmService.stopGenerating(agentId, taskId, requestBody);
    }

    /**
     * 提交消息反馈
     * 用于对消息进行评价（如点赞、点踩等）
     */
    @PostMapping("/messages/{messageId}/feedbacks")
    public Map<String, Object> submitMessageFeedback(
            @PathVariable String messageId,
            @RequestBody Map<String, Object> requestBody,
            @RequestParam(required = false) String agentId) {
        log.info("提交消息反馈，messageId: {}, feedback: {}, agentId: {}", messageId, requestBody, agentId);
        return llmService.submitMessageFeedback(agentId, messageId, requestBody);
    }
    
    /**
     * 获取消息建议
     * 用于获取特定消息的建议回复
     */
    @GetMapping("/messages/{messageId}/suggested")
    public Map<String, Object> getMessageSuggestions(
            @PathVariable String messageId,
            @RequestParam String user,
            @RequestParam(required = false) String agentId) {
        log.info("获取消息建议，messageId: {}, user: {}, agentId: {}", messageId, user, agentId);
        return llmService.getMessageSuggestions(agentId, messageId, user);
    }
    
    /**
     * 获取消息列表
     * 用于获取用户的消息历史记录
     */
    @GetMapping("/messages")
    public Map<String, Object> getMessages(
            @RequestParam(defaultValue = "1") String user,
            @RequestParam(required = false) String conversation_id,
            @RequestParam(required = false, defaultValue = "20") Integer limit,
            @RequestParam(required = false) String agentId) {
        log.info("获取消息列表，user: {}, conversation_id: {}, limit: {}, agentId: {}", user, conversation_id, limit, agentId);
        return llmService.getMessages(agentId, user, conversation_id, limit);
    }
    
    /**
     * 获取对话列表
     * 用于获取用户的对话历史记录
     */
    @GetMapping("/conversations")
    public Map<String, Object> getConversations(
            @RequestParam(defaultValue = "1") String user,
            @RequestParam(required = false) String last_id,
            @RequestParam(required = false, defaultValue = "20") Integer limit,
            @RequestParam(required = false) String keyWord,
            @RequestParam(required = false) String agentId) {
        log.info("获取对话列表，user: {}, last_id: {}, limit: {}, agentId: {}", user, last_id, limit, agentId);
        return llmService.getConversations(agentId, user, last_id, limit, keyWord);
    }
    
    /**
     * 删除对话
     * 用于删除指定的对话
     */
    @DeleteMapping("/conversations/{conversationId}")
    public Map<String, Object> deleteConversation(
            @PathVariable String conversationId,
            @RequestBody Map<String, Object> requestBody,
            @RequestParam(required = false) String agentId) {
        log.info("删除对话，conversationId: {}, requestBody: {}, agentId: {}", conversationId, requestBody, agentId);
        return llmService.deleteConversation(agentId, conversationId, requestBody);
    }
    
    /**
     * 重命名对话
     * 用于重命名指定的对话
     */
    @PostMapping("/conversations/{conversationId}/name")
    public Map<String, Object> renameConversation(
            @PathVariable String conversationId,
            @RequestBody Map<String, Object> requestBody,
            @RequestParam(required = false) String agentId) {
        log.info("重命名对话，conversationId: {}, requestBody: {}, agentId: {}", conversationId, requestBody, agentId);
        return llmService.renameConversation(agentId, conversationId, requestBody);
    }
    
    /**
     * 语音转文本
     * 用于将语音转换为文本
     */
    @PostMapping("/audio-to-text")
    public Map<String, Object> audioToText(
            @RequestParam("file") MultipartFile audioFile,
            @RequestParam(required = false) String agentId) {
        log.info("语音转文本，文件名: {}, 文件大小: {}, agentId: {}", audioFile.getOriginalFilename(), audioFile.getSize(), agentId);
        return llmService.audioToText(agentId, audioFile);
    }
    
    /**
     * 文本转语音
     * 用于将文本转换为语音
     */
    @PostMapping("/text-to-audio")
    public ResponseEntity<byte[]> textToAudio(
            @RequestBody Map<String, Object> requestBody,
            @RequestParam(required = false) String agentId) {
        log.info("文本转语音，requestBody: {}, agentId: {}", requestBody, agentId);
        byte[] audioData = llmService.textToAudio(agentId, requestBody);
        
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
    public Map<String, Object> getAppInfo(@RequestParam(required = false) String agentId) {
        log.info("获取应用基本信息，agentId: {}", agentId);
        return llmService.getAppInfo(agentId);
    }
    
    /**
     * 获取应用参数信息
     */
    @GetMapping("/parameters")
    public Map<String, Object> getParameters(@RequestParam(required = false) String agentId) {
        log.info("获取应用参数信息，agentId: {}", agentId);
        return llmService.getParameters(agentId);
    }
    
    /**
     * 获取应用元数据
     */
    @GetMapping("/meta")
    public Map<String, Object> getMeta(@RequestParam(required = false) String agentId) {
        log.info("获取应用元数据，agentId: {}", agentId);
        return llmService.getMeta(agentId);
    }

    /**
     * 上传文件
     * 用于上传图片等文件到系统
     */
    @PostMapping("/files/upload")
    public Map<String, Object> uploadFile(
            @RequestParam("file") MultipartFile file,
            @RequestParam("user") String user,
            @RequestParam(required = false) String agentId) {
        log.info("上传文件，文件名: {}, 文件大小: {}, 用户: {}, agentId: {}", file.getOriginalFilename(), file.getSize(), user, agentId);
        return llmService.uploadFile(agentId, file, user);
    }

}
