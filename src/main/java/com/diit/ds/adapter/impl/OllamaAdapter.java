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
import org.springframework.web.multipart.MultipartFile;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.ConcurrentHashMap;

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
    
    // 存储任务ID和对应的连接，用于停止生成
    private final ConcurrentHashMap<String, HttpURLConnection> activeConnections = new ConcurrentHashMap<>();

    @Value("${ollama.api.base-url:http://localhost:11434}")
    private String ollamaBaseUrl;

    @Override
    public Map<String, Object> processBlockingResponse(Map<String, Object> requestBody) {
        // 转换请求格式为Ollama格式
        Map<String, Object> ollamaRequest = convertToOllamaFormat(requestBody);
        
        HttpHeaders headers = createHeaders();
        HttpEntity<Map<String, Object>> entity = new HttpEntity<>(ollamaRequest, headers);

        ResponseEntity<Map> response = restTemplate.exchange(
                ollamaBaseUrl + "/api/generate",
                HttpMethod.POST,
                entity,
                Map.class
        );
        log.info("Response from Ollama API: {}", response.getBody());

        // 转换响应格式为统一格式
        Map<String, Object> ollamaResponse = response.getBody();
        return convertFromOllamaFormat(ollamaResponse);
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
                
                // 如果请求中包含任务ID，则存储连接以便后续停止
                String taskId = requestBody.containsKey("task_id") ? requestBody.get("task_id").toString() : null;
                if (taskId != null) {
                    activeConnections.put(taskId, connection);
                }

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
                
                // 清理连接
                if (taskId != null) {
                    activeConnections.remove(taskId);
                }

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
    
    @Override
    public Map<String, Object> stopGenerating(String taskId, Map<String, Object> requestBody) {
        // Ollama没有直接的停止API，我们通过关闭连接来实现
        HttpURLConnection connection = activeConnections.get(taskId);
        if (connection != null) {
            try {
                connection.disconnect();
                activeConnections.remove(taskId);
                log.info("已停止Ollama生成任务: {}", taskId);
                return Map.of("result", "success");
            } catch (Exception e) {
                log.error("停止Ollama生成任务失败: {}", taskId, e);
                return Map.of("result", "error", "message", e.getMessage());
            }
        } else {
            log.warn("未找到要停止的Ollama生成任务: {}", taskId);
            return Map.of("result", "not_found", "message", "未找到指定的生成任务");
        }
    }
    
    @Override
    public Map<String, Object> submitMessageFeedback(String messageId, Map<String, Object> requestBody) {
        // Ollama没有反馈API，我们只记录反馈信息
        log.info("收到消息反馈请求，但Ollama不支持反馈功能。消息ID: {}, 反馈内容: {}", messageId, requestBody);
        
        // 返回成功响应，保持与Dify API一致的接口
        return Map.of("result", "success", "message", "Feedback recorded (Ollama does not support feedback)");
    }
    
    @Override
    public Map<String, Object> getMessageSuggestions(String messageId, String user) {
        // Ollama没有建议API，我们返回一些默认建议
        log.info("收到获取消息建议请求，但Ollama不支持此功能。消息ID: {}, 用户: {}", messageId, user);
        
        // 返回一些默认建议，保持与Dify API一致的接口
        return Map.of(
            "result", "success",
            "data", List.of(
                "请继续",
                "能详细解释一下吗？",
                "谢谢，这很有帮助"
            )
        );
    }
    
    @Override
    public Map<String, Object> getMessages(String user, String conversationId, Integer limit) {
        // Ollama没有消息历史API，我们返回一个空列表
        log.info("收到获取消息列表请求，但Ollama不支持此功能。用户: {}, 对话ID: {}, 限制: {}", user, conversationId, limit);
        
        // 返回一个空列表，保持与Dify API一致的接口
        return Map.of(
            "limit", limit != null ? limit : 20,
            "has_more", false,
            "data", List.of()
        );
    }
    
    @Override
    public Map<String, Object> getConversations(String user, String lastId, Integer limit) {
        // Ollama没有对话历史API，我们返回一个空列表
        log.info("收到获取对话列表请求，但Ollama不支持此功能。用户: {}, 最后ID: {}, 限制: {}", user, lastId, limit);
        
        // 返回一个空列表，保持与Dify API一致的接口
        return Map.of(
            "limit", limit != null ? limit : 20,
            "has_more", false,
            "data", List.of()
        );
    }
    
    @Override
    public Map<String, Object> deleteConversation(String conversationId, Map<String, Object> requestBody) {
        // Ollama没有对话管理API，我们只记录请求
        log.info("收到删除对话请求，但Ollama不支持此功能。对话ID: {}, 请求体: {}", conversationId, requestBody);
        
        // 返回成功响应，保持与Dify API一致的接口
        return Map.of("result", "success");
    }
    
    @Override
    public Map<String, Object> renameConversation(String conversationId, Map<String, Object> requestBody) {
        // Ollama没有对话管理API，我们只记录请求
        log.info("收到重命名对话请求，但Ollama不支持此功能。对话ID: {}, 请求体: {}", conversationId, requestBody);
        
        // 返回一个模拟的对话信息，保持与Dify API一致的接口
        return Map.of(
            "id", conversationId,
            "name", requestBody.containsKey("name") ? requestBody.get("name") : "New chat",
            "inputs", Map.of(),
            "status", "normal",
            "introduction", "",
            "created_at", System.currentTimeMillis() / 1000,
            "updated_at", System.currentTimeMillis() / 1000
        );
    }
    
    @Override
    public Map<String, Object> audioToText(MultipartFile audioFile) {
        // Ollama没有语音转文本API，我们只记录请求
        log.info("收到语音转文本请求，但Ollama不支持此功能。文件名: {}, 文件大小: {}", 
                audioFile.getOriginalFilename(), audioFile.getSize());
        
        // 返回一个模拟的转换结果，保持与Dify API一致的接口
        return Map.of("text", "Ollama does not support audio to text conversion");
    }
    
    @Override
    public byte[] textToAudio(Map<String, Object> requestBody) {
        // Ollama没有文本转语音API，我们只记录请求
        log.info("收到文本转语音请求，但Ollama不支持此功能。请求体: {}", requestBody);
        
        // 返回一个空的音频数据
        return new byte[0];
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
            Object inputsObj = requestBody.get("inputs");
            StringBuilder prompt = new StringBuilder();
            
            if (inputsObj instanceof List) {
                // 处理列表格式的输入
                List<Map<String, Object>> messages = (List<Map<String, Object>>) inputsObj;
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
            } else if (inputsObj instanceof Map) {
                // 处理映射格式的输入
                Map<String, Object> inputsMap = (Map<String, Object>) inputsObj;
                if (inputsMap.containsKey("messages")) {
                    List<Map<String, Object>> messages = (List<Map<String, Object>>) inputsMap.get("messages");
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
                } else {
                    // 直接使用输入作为提示
                    prompt.append(inputsMap.toString());
                }
            } else {
                // 直接使用输入作为提示
                prompt.append(inputsObj.toString());
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

    @Override
    public Map<String, Object> getAppInfo() {
        // Ollama没有应用信息API，我们只记录请求
        log.info("收到获取应用信息请求，但Ollama不支持此功能");
        
        // 返回一个备用响应，保持与Dify API一致的接口
        return Map.of(
            "name", "Ollama App",
            "description", "Ollama不支持应用信息API，这是一个备用响应",
            "tags", List.of("ollama")
        );
    }
    
    @Override
    public Map<String, Object> getParameters() {
        // Ollama没有参数信息API，我们只记录请求
        log.info("收到获取应用参数信息请求，但Ollama不支持此功能");
        
        // 返回一个备用响应，保持与Dify API一致的接口
        Map<String, Object> textInput = new HashMap<>();
        textInput.put("label", "提示词");
        textInput.put("variable", "prompt");
        textInput.put("required", true);
        textInput.put("max_length", 100);
        textInput.put("default", "");
        
        Map<String, Object> textInputWrapper = new HashMap<>();
        textInputWrapper.put("text-input", textInput);
        
        Map<String, Object> imageUpload = new HashMap<>();
        imageUpload.put("enabled", true);
        imageUpload.put("number_limits", 3);
        imageUpload.put("transfer_methods", List.of("remote_url", "local_file"));
        
        Map<String, Object> fileUpload = new HashMap<>();
        fileUpload.put("image", imageUpload);
        
        Map<String, Object> systemParameters = new HashMap<>();
        systemParameters.put("file_size_limit", 15);
        systemParameters.put("image_file_size_limit", 10);
        systemParameters.put("audio_file_size_limit", 50);
        systemParameters.put("video_file_size_limit", 100);
        
        return Map.of(
            "introduction", "Ollama模型，欢迎使用",
            "user_input_form", List.of(textInputWrapper),
            "file_upload", fileUpload,
            "system_parameters", systemParameters
        );
    }
    
    @Override
    public Map<String, Object> getMeta() {
        // Ollama没有元数据API，我们只记录请求
        log.info("收到获取应用元数据请求，但Ollama不支持此功能");
        
        // 返回一个备用响应，保持与Dify API一致的接口
        Map<String, Object> apiTool = new HashMap<>();
        apiTool.put("background", "#252525");
        apiTool.put("content", "😊");
        
        Map<String, Object> toolIcons = new HashMap<>();
        toolIcons.put("api_tool", apiTool);
        
        return Map.of(
            "tool_icons", toolIcons
        );
    }
    
    @Override
    public Map<String, Object> uploadFile(MultipartFile file, String user) {
        // Ollama没有文件上传API，我们只记录请求
        log.info("收到文件上传请求，但Ollama不支持此功能。文件名: {}, 用户: {}", 
                file.getOriginalFilename(), user);
        
        // 返回一个备用响应，保持与Dify API一致的接口
        return Map.of(
            "id", "file_" + System.currentTimeMillis(),
            "name", file.getOriginalFilename(),
            "size", file.getSize(),
            "mime_type", file.getContentType(),
            "url", "/mock/file/url",
            "user", user,
            "created_at", System.currentTimeMillis() / 1000
        );
    }

    /**
     * 创建请求头
     */
    private HttpHeaders createHeaders() {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        // Ollama通常不需要授权，但如果需要，可以在这里添加
        return headers;
    }
} 