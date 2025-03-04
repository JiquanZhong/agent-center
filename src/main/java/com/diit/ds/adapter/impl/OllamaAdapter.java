package com.diit.ds.adapter.impl;

import com.diit.ds.adapter.LLMAdapter;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;

/**
 * Ollama适配器实现
 * 适配Ollama API的具体实现
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class OllamaAdapter implements LLMAdapter {

    private final ObjectMapper objectMapper;
    private final RestTemplate restTemplate;
    private final ExecutorService executorService;

    @Value("${ollama.api.base-url:http://localhost:11434}")
    private String ollamaBaseUrl;

    @Override
    public Map<String, Object> processBlockingResponse(Map<String, Object> requestBody) {
        // 转换请求格式为Ollama格式
        Map<String, Object> ollamaRequest = convertToOllamaFormat(requestBody);
        
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        // Ollama通常不需要授权，但如果需要，可以在这里添加

        HttpEntity<Map<String, Object>> entity = new HttpEntity<>(ollamaRequest, headers);

        ResponseEntity<Map> response = restTemplate.exchange(
                ollamaBaseUrl + "/api/generate",
                HttpMethod.POST,
                entity,
                Map.class
        );
        log.info("Response from Ollama API: {}", response.getBody());

        // 转换响应格式为统一格式
        return convertFromOllamaFormat(response.getBody());
    }

    @Override
    public void processStreamingResponse(Map<String, Object> requestBody, SseEmitter emitter) {
        executorService.execute(() -> {
            HttpURLConnection connection = null;
            try {
                // 转换请求格式为Ollama格式
                Map<String, Object> ollamaRequest = convertToOllamaFormat(requestBody);
                // Ollama默认是流式的，所以不需要额外设置
                
                // 准备连接
                URL url = new URL(ollamaBaseUrl + "/api/generate");
                connection = (HttpURLConnection) url.openConnection();
                connection.setRequestMethod("POST");
                connection.setRequestProperty("Content-Type", "application/json");
                // Ollama通常不需要授权，但如果需要，可以在这里添加
                connection.setDoOutput(true);
                connection.setReadTimeout(0); // 无限超时

                // 写入请求体
                String requestBodyJson = objectMapper.writeValueAsString(ollamaRequest);
                connection.getOutputStream().write(requestBodyJson.getBytes());

                // 读取响应
                int responseCode = connection.getResponseCode();
                if (responseCode == HttpURLConnection.HTTP_OK) {
                    try (BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream()))) {
                        String line;
                        while ((line = reader.readLine()) != null) {
                            // Ollama返回的是JSON行
                            Map<String, Object> ollamaResponse = objectMapper.readValue(line, Map.class);
                            Map<String, Object> unifiedResponse = convertStreamFromOllamaFormat(ollamaResponse);
                            
                            // 发送转换后的数据
                            emitter.send(SseEmitter.event().data(objectMapper.writeValueAsString(unifiedResponse), MediaType.APPLICATION_JSON));
                            
                            // 如果是最后一个响应，则结束
                            if (ollamaResponse.containsKey("done") && (Boolean)ollamaResponse.get("done")) {
                                break;
                            }
                        }
                    }
                } else {
                    // 处理错误
                    Map<String, Object> errorResponse = Map.of(
                            "error", Map.of(
                                    "message", "Error from Ollama API: " + responseCode,
                                    "status_code", responseCode
                            )
                    );
                    emitter.send(SseEmitter.event().data(objectMapper.writeValueAsString(errorResponse), MediaType.APPLICATION_JSON));
                }

                // 完成
                emitter.complete();

            } catch (Exception e) {
                try {
                    log.error("Error processing streaming response", e);
                    Map<String, Object> errorResponse = Map.of(
                            "error", Map.of(
                                    "message", "Error processing streaming response: " + e.getMessage(),
                                    "status_code", 500
                            )
                    );
                    emitter.send(SseEmitter.event().data(objectMapper.writeValueAsString(errorResponse), MediaType.APPLICATION_JSON));
                    emitter.completeWithError(e);
                } catch (IOException ex) {
                    // 忽略发送错误的异常
                    log.error("Error sending error response", ex);
                }
            } finally {
                if (connection != null) {
                    connection.disconnect();
                }
            }
        });
    }
    
    /**
     * 将统一格式的请求转换为Ollama格式
     */
    private Map<String, Object> convertToOllamaFormat(Map<String, Object> requestBody) {
        Map<String, Object> ollamaRequest = new HashMap<>();
        
        // 设置模型
        ollamaRequest.put("model", requestBody.getOrDefault("model", "llama2"));
        
        // 转换消息格式为提示
        if (requestBody.containsKey("inputs")) {
            List<Map<String, Object>> messages = (List<Map<String, Object>>) requestBody.get("inputs");
            StringBuilder prompt = new StringBuilder();
            
            for (Map<String, Object> message : messages) {
                String role = (String) message.get("role");
                String content = (String) message.get("content");
                
                if ("user".equals(role)) {
                    prompt.append("User: ").append(content).append("\n");
                } else if ("assistant".equals(role)) {
                    prompt.append("Assistant: ").append(content).append("\n");
                } else if ("system".equals(role)) {
                    prompt.append("System: ").append(content).append("\n");
                }
            }
            
            ollamaRequest.put("prompt", prompt.toString());
        }
        
        // 设置温度等参数
        if (requestBody.containsKey("parameters")) {
            Map<String, Object> parameters = (Map<String, Object>) requestBody.get("parameters");
            if (parameters.containsKey("temperature")) {
                ollamaRequest.put("temperature", parameters.get("temperature"));
            }
            if (parameters.containsKey("top_p")) {
                ollamaRequest.put("top_p", parameters.get("top_p"));
            }
            if (parameters.containsKey("max_tokens")) {
                ollamaRequest.put("num_predict", parameters.get("max_tokens"));
            }
        }
        
        return ollamaRequest;
    }
    
    /**
     * 将Ollama格式的响应转换为统一格式
     */
    private Map<String, Object> convertFromOllamaFormat(Map<String, Object> ollamaResponse) {
        Map<String, Object> unifiedResponse = new HashMap<>();
        
        // 设置ID（Ollama可能没有ID，使用时间戳代替）
        unifiedResponse.put("id", "ollama-" + System.currentTimeMillis());
        
        // 设置创建时间
        unifiedResponse.put("created", System.currentTimeMillis() / 1000);
        
        // 设置模型
        unifiedResponse.put("model", ollamaResponse.get("model"));
        
        // 设置回答
        unifiedResponse.put("answer", ollamaResponse.get("response"));
        
        // 设置元数据
        Map<String, Object> metadata = new HashMap<>();
        if (ollamaResponse.containsKey("eval_count")) {
            metadata.put("eval_count", ollamaResponse.get("eval_count"));
        }
        if (ollamaResponse.containsKey("eval_duration")) {
            metadata.put("eval_duration", ollamaResponse.get("eval_duration"));
        }
        unifiedResponse.put("metadata", metadata);
        
        return unifiedResponse;
    }
    
    /**
     * 将Ollama流式格式的响应转换为统一格式
     */
    private Map<String, Object> convertStreamFromOllamaFormat(Map<String, Object> ollamaStreamResponse) {
        Map<String, Object> unifiedResponse = new HashMap<>();
        
        // 设置ID（Ollama可能没有ID，使用时间戳代替）
        unifiedResponse.put("id", "ollama-" + System.currentTimeMillis());
        
        // 设置创建时间
        unifiedResponse.put("created", System.currentTimeMillis() / 1000);
        
        // 设置模型
        unifiedResponse.put("model", ollamaStreamResponse.get("model"));
        
        // 设置回答
        unifiedResponse.put("answer", ollamaStreamResponse.get("response"));
        
        // 设置元数据
        Map<String, Object> metadata = new HashMap<>();
        if (ollamaStreamResponse.containsKey("done")) {
            metadata.put("done", ollamaStreamResponse.get("done"));
        }
        unifiedResponse.put("metadata", metadata);
        
        return unifiedResponse;
    }
} 