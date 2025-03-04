package com.diit.ds.controller;

import com.diit.ds.service.LLMService;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.Map;

@Slf4j
@Tag(name = "智能体的Controller，接口为Dify的调用格式")
@RestController
@RequestMapping("/v1")
@RequiredArgsConstructor
public class DifyTypeController {

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
}
