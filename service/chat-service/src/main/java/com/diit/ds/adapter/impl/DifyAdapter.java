package com.diit.ds.adapter.impl;

import com.diit.ds.adapter.LLMAdapter;
import com.diit.ds.config.DifyConfig;
import com.diit.ds.context.UserContext;
import com.diit.ds.service.MessagesService;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.util.UriComponentsBuilder;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;

/**
 * Dify适配器实现
 * 适配Dify API的具体实现
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class DifyAdapter implements LLMAdapter {

    private final ObjectMapper objectMapper;
    private final RestTemplate restTemplate;
    private final ExecutorService executorService;
    private final DifyConfig difyConfig;
    private final MessagesService messagesService;
    @Value("${dify.api.base-url:http://192.168.11.205}")
    private String difyBaseUrl;

    @Override
    public Map<String, Object> processBlockingResponse(Map<String, Object> requestBody) {
        String url = difyBaseUrl + "/v1/chat-messages";

        // 发送请求
        HttpHeaders headers = createHeaders();
        HttpEntity<Map<String, Object>> entity = new HttpEntity<>(requestBody, headers);

        try {
            ResponseEntity<Map> response = restTemplate.exchange(
                    url,
                    HttpMethod.POST,
                    entity,
                    Map.class
            );

            return response.getBody();
        } catch (Exception e) {
            log.error("处理阻塞响应失败", e);
            throw new RuntimeException("处理阻塞响应失败: " + e.getMessage());
        }
    }

    @Override
    public void processStreamingResponse(Map<String, Object> requestBody, SseEmitter emitter) {
        executorService.execute(() -> {
            HttpURLConnection connection = null;
            try {
                // 准备连接
                URL url = new URL(difyBaseUrl + "/v1/chat-messages");
                connection = (HttpURLConnection) url.openConnection();
                connection.setRequestMethod("POST");
                connection.setRequestProperty("Content-Type", "application/json");
                connection.setRequestProperty("Authorization", difyConfig.getChatAgentToken());
                connection.setDoOutput(true);
                connection.setReadTimeout(0); // 无限超时

                // 写入请求体
                String requestBodyJson = objectMapper.writeValueAsString(requestBody);
                connection.getOutputStream().write(requestBodyJson.getBytes());

                // 读取响应
                int responseCode = connection.getResponseCode();
                if (responseCode == HttpURLConnection.HTTP_OK) {
                    try (BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream()))) {
                        String line;
                        while ((line = reader.readLine()) != null) {
                            if (line.startsWith("data: ")) {
                                String data = line.substring(6);
                                // 直接将数据发送给客户端
                                emitter.send(SseEmitter.event().data(data, MediaType.APPLICATION_JSON));
                            }
                        }
                    }
                } else {
                    // 处理错误
                    log.error("Response code: {}, message: {}", responseCode, connection.getResponseMessage());
                    Map<String, Object> errorResponse = Map.of(
                            "error", Map.of(
                                    "message", "Error from Dify API: " + responseCode,
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

    @Override
    public Map<String, Object> stopGenerating(String taskId, Map<String, Object> requestBody) {
        String url = difyBaseUrl + "/v1/chat-messages/" + taskId + "/stop";

        // 发送请求
        HttpHeaders headers = createHeaders();
        HttpEntity<Map<String, Object>> entity = new HttpEntity<>(requestBody, headers);

        // 如果没有user参数，添加一个默认值
        if (!requestBody.containsKey("user")) {
            requestBody.put("user", "undefined");  // 使用适当的用户标识
        }

        try {
            ResponseEntity<Map> response = restTemplate.exchange(
                    url,
                    HttpMethod.POST,
                    entity,
                    Map.class
            );

            return response.getBody();
        } catch (Exception e) {
            log.error("停止生成响应失败", e);
            throw new RuntimeException("停止生成响应失败: " + e.getMessage());
        }
    }

    @Override
    public Map<String, Object> submitMessageFeedback(String messageId, Map<String, Object> requestBody) {
        String url = difyBaseUrl + "/v1/messages/" + messageId + "/feedbacks";

        // 发送请求
        HttpHeaders headers = createHeaders();
        HttpEntity<Map<String, Object>> entity = new HttpEntity<>(requestBody, headers);

        try {
            ResponseEntity<Map> response = restTemplate.exchange(
                    url,
                    HttpMethod.POST,
                    entity,
                    Map.class
            );

            return response.getBody();

        } catch (Exception e) {
            log.error("提交消息反馈失败", e);
            throw new RuntimeException("提交消息反馈失败: " + e.getMessage());
        }
    }

    @Override
    public Map<String, Object> getMessageSuggestions(String messageId, String user) {
        String url = difyBaseUrl + "/v1/messages/" + messageId + "/suggested?user=" + user;

        // 发送请求
        HttpHeaders headers = createHeaders();
        HttpEntity<String> entity = new HttpEntity<>(headers);

        try {
            ResponseEntity<Map> response = restTemplate.exchange(
                    url,
                    HttpMethod.GET,
                    entity,
                    Map.class
            );

            return response.getBody();
        } catch (Exception e) {
            log.error("获取消息建议失败", e);
            throw new RuntimeException("获取消息建议失败: " + e.getMessage());
        }
    }

    @Override
    public Map<String, Object> getMessages(String user, String conversationId, Integer limit) {
        String url = difyBaseUrl + "/v1/messages";

        // 构建请求参数
        UriComponentsBuilder builder = UriComponentsBuilder.fromHttpUrl(url);
        builder.queryParam("user", user);
        if (conversationId != null && !conversationId.isEmpty()) {
            builder.queryParam("conversation_id", conversationId);
        }
        if (limit != null) {
            builder.queryParam("limit", limit);
        }

        // 发送请求
        HttpHeaders headers = createHeaders();
        HttpEntity<String> entity = new HttpEntity<>(headers);

        try {
            ResponseEntity<Map> response = restTemplate.exchange(
                    builder.toUriString(),
                    HttpMethod.GET,
                    entity,
                    Map.class
            );

            // 处理消息排序，确保新消息排在后，旧的消息排在前面
            Map<String, Object> responseBody = response.getBody();
            if (responseBody != null && responseBody.containsKey("data")) {
                List<Map<String, Object>> messages = (List<Map<String, Object>>) responseBody.get("data");
                if (messages != null && !messages.isEmpty()) {
                    // 按created_at字段降序排序（新消息在前）
                    messages.sort((m1, m2) -> {
                        Long time1 = m1.containsKey("created_at") ? Long.valueOf(m1.get("created_at").toString()) : 0L;
                        Long time2 = m2.containsKey("created_at") ? Long.valueOf(m2.get("created_at").toString()) : 0L;
                        return time1.compareTo(time2); // 降序排列
                    });
                    // 过滤掉answer为空字符串的消息和error有值的消息
                    List<Map<String, Object>> filteredMessages = messages.stream()
                            .filter(message -> {
                                Object answer = message.get("answer");
                                Object error = message.get("error");
                                return answer != null && !answer.toString().isEmpty() && (error == null || error.toString().isEmpty());
                            })
                            .collect(java.util.stream.Collectors.toList());
                    responseBody.put("data", filteredMessages);
                    
                    try {
                        // 收集所有消息ID，以便在数据库中查询对应的workflowRunId
                        if (!filteredMessages.isEmpty()) {
                            List<String> messageIds = filteredMessages.stream()
                                    .map(message -> message.get("id").toString())
                                    .collect(java.util.stream.Collectors.toList());
                            
                            // 查询消息对应的workflowRunId
                            Map<String, Object> workflowRunIds = messagesService.getWorkflowRunIdsByMessageIds(messageIds);
                            
                            // 为每条消息添加work_flow_run_id字段
                            for (Map<String, Object> message : filteredMessages) {
                                String messageId = message.get("id").toString();
                                if (workflowRunIds.containsKey(messageId)) {
                                    message.put("work_flow_run_id", workflowRunIds.get(messageId));
                                } else {
                                    message.put("work_flow_run_id", null);
                                }
                            }
                        }
                    } catch (Exception e) {
                        // 如果获取workflowRunId失败，记录日志但继续处理
                        log.error("获取workflowRunId失败，继续返回原始消息", e);
                        // 为每条消息添加空的work_flow_run_id字段
                        for (Map<String, Object> message : filteredMessages) {
                            message.put("work_flow_run_id", null);
                        }
                    }
                }
            }

            return responseBody;
        } catch (Exception e) {
            log.error("获取消息列表失败", e);
            throw new RuntimeException("获取消息列表失败: " + e.getMessage());
        }
    }

    @Override
    public Map<String, Object> getConversations(String user, String lastId, Integer limit) {
        String url = difyBaseUrl + "/v1/conversations";

        // 构建请求参数
        UriComponentsBuilder builder = UriComponentsBuilder.fromHttpUrl(url);
        if (user != null && !user.isEmpty()) {
            builder.queryParam("user", user);
        } else {
            // 根据jwt获取userId，防止报错
            builder.queryParam("user", UserContext.getUserId());
        }
        if (lastId != null && !lastId.isEmpty()) {
            builder.queryParam("last_id", lastId);
        }
        if (limit != null) {
            builder.queryParam("limit", limit);
        }

        // 发送请求
        HttpHeaders headers = createHeaders();
        HttpEntity<String> entity = new HttpEntity<>(headers);

        try {
            ResponseEntity<Map> response = restTemplate.exchange(
                    builder.toUriString(),
                    HttpMethod.GET,
                    entity,
                    Map.class
            );

            return response.getBody();
        } catch (Exception e) {
            log.error("获取对话列表失败", e);
            throw new RuntimeException("获取对话列表失败: " + e.getMessage());
        }
    }

    @Override
    public Map<String, Object> getConversations(String user, String lastId, Integer limit, String keyword) {
        if (keyword == null || keyword.isEmpty()) {
            return this.getConversations(user, lastId, limit);
        }

        // 获取四倍limit的数据
        Map<String, Object> conversations = this.getConversations(user, lastId, 100); // 超过100dify报错
        
        // 获取原始数据列表
        List<Map<String, Object>> data = (List<Map<String, Object>>) conversations.get("data");
        if (data == null || data.isEmpty()) {
            return conversations;
        }

        // 根据keyword筛选数据
        List<Map<String, Object>> filteredData = data.stream()
                .filter(item -> {
                    String name = (String) item.get("name");
                    return name != null && name.toLowerCase().contains(keyword.toLowerCase());
                })
                .limit(limit) // 限制返回数量为原始limit
                .toList();

        // 更新返回结果
        conversations.put("data", filteredData);
        conversations.put("has_more", filteredData.size() >= limit);
        conversations.put("limit", limit);

        return conversations;
    }

    @Override
    public Map<String, Object> deleteConversation(String conversationId, Map<String, Object> requestBody) {
        String url = difyBaseUrl + "/v1/conversations/" + conversationId;

        // 发送请求
        HttpHeaders headers = createHeaders();
        HttpEntity<Map<String, Object>> entity = new HttpEntity<>(requestBody, headers);

        try {
            ResponseEntity<Map> response = restTemplate.exchange(
                    url,
                    HttpMethod.DELETE,
                    entity,
                    Map.class
            );

            return response.getBody();
        } catch (Exception e) {
            log.error("删除对话失败", e);
            throw new RuntimeException("删除对话失败: " + e.getMessage());
        }
    }

    @Override
    public Map<String, Object> renameConversation(String conversationId, Map<String, Object> requestBody) {
        String url = difyBaseUrl + "/v1/conversations/" + conversationId + "/name";

        // 发送请求
        HttpHeaders headers = createHeaders();
        HttpEntity<Map<String, Object>> entity = new HttpEntity<>(requestBody, headers);

        try {
            ResponseEntity<Map> response = restTemplate.exchange(
                    url,
                    HttpMethod.POST,
                    entity,
                    Map.class
            );

            return response.getBody();
        } catch (Exception e) {
            log.error("重命名对话失败", e);
            throw new RuntimeException("重命名对话失败: " + e.getMessage());
        }
    }

    @Override
    public Map<String, Object> audioToText(MultipartFile audioFile) {
        String url = difyBaseUrl + "/v1/audio-to-text";

        try {
            // 创建MultipartFile资源
            ByteArrayResource resource = new ByteArrayResource(audioFile.getBytes()) {
                @Override
                public String getFilename() {
                    return audioFile.getOriginalFilename();
                }
            };

            // 创建MultipartForm请求
            MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
            body.add("file", resource);

            HttpHeaders headers = createHeaders();
            headers.setContentType(MediaType.MULTIPART_FORM_DATA);

            HttpEntity<MultiValueMap<String, Object>> entity = new HttpEntity<>(body, headers);

            ResponseEntity<Map> response = restTemplate.exchange(
                    url,
                    HttpMethod.POST,
                    entity,
                    Map.class
            );

            return response.getBody();
        } catch (Exception e) {
            log.error("语音转文本失败", e);
            throw new RuntimeException("语音转文本失败: " + e.getMessage());
        }
    }

    @Override
    public byte[] textToAudio(Map<String, Object> requestBody) {
        String url = difyBaseUrl + "/v1/text-to-audio";

        // 发送请求
        HttpHeaders headers = createHeaders();
        HttpEntity<Map<String, Object>> entity = new HttpEntity<>(requestBody, headers);

        try {
            ResponseEntity<byte[]> response = restTemplate.exchange(
                    url,
                    HttpMethod.POST,
                    entity,
                    byte[].class
            );

            return response.getBody();
        } catch (Exception e) {
            log.error("文本转语音失败", e);
            throw new RuntimeException("文本转语音失败: " + e.getMessage());
        }
    }

    @Override
    public Map<String, Object> getAppInfo() {
        String url = difyBaseUrl + "/v1/info";

        // 发送请求
        HttpHeaders headers = createHeaders();
        HttpEntity<String> entity = new HttpEntity<>(headers);

        try {
            ResponseEntity<Map> response = restTemplate.exchange(
                    url,
                    HttpMethod.GET,
                    entity,
                    Map.class
            );

            return response.getBody();
        } catch (Exception e) {
            log.error("获取应用信息失败", e);
            throw new RuntimeException("获取应用信息失败: " + e.getMessage());
        }
    }

    @Override
    public Map<String, Object> getParameters() {
        String url = difyBaseUrl + "/v1/parameters";

        // 发送请求
        HttpHeaders headers = createHeaders();
        HttpEntity<String> entity = new HttpEntity<>(headers);

        try {
            ResponseEntity<Map> response = restTemplate.exchange(
                    url,
                    HttpMethod.GET,
                    entity,
                    Map.class
            );

            return response.getBody();
        } catch (Exception e) {
            log.error("获取应用参数信息失败", e);
            throw new RuntimeException("获取应用参数信息失败: " + e.getMessage());
        }
    }

    @Override
    public Map<String, Object> getMeta() {
        String url = difyBaseUrl + "/v1/meta";

        HttpHeaders headers = createHeaders();
        HttpEntity<String> entity = new HttpEntity<>(headers);

        ResponseEntity<Map> response = restTemplate.exchange(
                url,
                HttpMethod.GET,
                entity,
                Map.class
        );

        return response.getBody();
    }

    @Override
    public Map<String, Object> uploadFile(MultipartFile file, String user) {
        String url = difyBaseUrl + "/v1/files/upload";

        try {
            // 创建MultipartFile资源
            ByteArrayResource resource = new ByteArrayResource(file.getBytes()) {
                @Override
                public String getFilename() {
                    return file.getOriginalFilename();
                }
            };

            // 创建MultipartForm请求
            MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
            body.add("file", resource);
            body.add("user", user);

            HttpHeaders headers = createHeaders();
            headers.setContentType(MediaType.MULTIPART_FORM_DATA);

            HttpEntity<MultiValueMap<String, Object>> entity = new HttpEntity<>(body, headers);

            ResponseEntity<Map> response = restTemplate.exchange(
                    url,
                    HttpMethod.POST,
                    entity,
                    Map.class
            );

            return response.getBody();
        } catch (Exception e) {
            log.error("文件上传失败", e);
            throw new RuntimeException("文件上传失败: " + e.getMessage());
        }
    }

    /**
     * 创建请求头
     */
    private HttpHeaders createHeaders() {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("Authorization", difyConfig.getChatAgentToken());
        return headers;
    }
}