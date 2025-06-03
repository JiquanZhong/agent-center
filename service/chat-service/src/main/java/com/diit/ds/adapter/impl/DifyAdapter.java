package com.diit.ds.adapter.impl;

import com.diit.ds.adapter.LLMAdapter;
import com.diit.ds.config.DifyConfig;
import com.diit.ds.context.UserContext;
import com.diit.ds.service.AgentsService;
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
    private final AgentsService agentsService;

    @Override
    public Map<String, Object> processBlockingResponse(String agentId, Map<String, Object> requestBody) {
        String url = difyBaseUrl + "/v1/chat-messages";

        // 发送请求
        HttpHeaders headers = createHeaders(agentId);
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
    public void processStreamingResponse(String agentId, Map<String, Object> requestBody, SseEmitter emitter) {
        executorService.execute(() -> {
            HttpURLConnection connection = null;
            try {
                // 准备连接
                URL url = new URL(difyBaseUrl + "/v1/chat-messages");
                HttpHeaders headers = createHeaders(agentId);
                connection = (HttpURLConnection) url.openConnection();
                connection.setRequestMethod("POST");
                connection.setRequestProperty("Content-Type", headers.getContentType().toString());
                connection.setRequestProperty("Authorization", headers.getFirst("Authorization"));
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
    public Map<String, Object> stopGenerating(String agentId, String taskId, Map<String, Object> requestBody) {
        String url = difyBaseUrl + "/v1/chat-messages/" + taskId + "/stop";

        // 发送请求
        HttpHeaders headers = createHeaders(agentId);
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
    public Map<String, Object> submitMessageFeedback(String agentId, String messageId, Map<String, Object> requestBody) {
        String url = difyBaseUrl + "/v1/messages/" + messageId + "/feedbacks";

        // 发送请求
        HttpHeaders headers = createHeaders(agentId);
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
    public Map<String, Object> getMessageSuggestions(String agentId, String messageId, String user) {
        String url = difyBaseUrl + "/v1/messages/" + messageId + "/suggested?user=" + user;

        // 发送请求
        HttpHeaders headers = createHeaders(agentId);
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
    public Map<String, Object> getMessages(String agentId, String user, String conversationId, Integer limit) {
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
        HttpHeaders headers = createHeaders(agentId);
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
    public Map<String, Object> getConversations(String agentId, String user, String lastId, Integer limit) {
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
        HttpHeaders headers = createHeaders(agentId);
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
    public Map<String, Object> getConversations(String agentId, String user, String lastId, Integer limit, String keyword) {
        if (keyword == null || keyword.isEmpty()) {
            return this.getConversations(agentId, user, lastId, limit);
        }

        // 获取四倍limit的数据
        Map<String, Object> conversations = this.getConversations(agentId, user, lastId, 100); // 超过100dify报错

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
    public Map<String, Object> deleteConversation(String agentId, String conversationId, Map<String, Object> requestBody) {
        String url = difyBaseUrl + "/v1/conversations/" + conversationId;

        // 发送请求
        HttpHeaders headers = createHeaders(agentId);
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
    public Map<String, Object> renameConversation(String agentId, String conversationId, Map<String, Object> requestBody) {
        String url = difyBaseUrl + "/v1/conversations/" + conversationId + "/name";

        // 发送请求
        HttpHeaders headers = createHeaders(agentId);
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
    public Map<String, Object> audioToText(String agentId, MultipartFile audioFile) {
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

            HttpHeaders headers = createHeaders(agentId);
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
    public byte[] textToAudio(String agentId, Map<String, Object> requestBody) {
        String url = difyBaseUrl + "/v1/text-to-audio";

        // 发送请求
        HttpHeaders headers = createHeaders(agentId);
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
    public Map<String, Object> getAppInfo(String agentId) {
        String url = difyBaseUrl + "/v1/info";

        // 发送请求
        HttpHeaders headers = createHeaders(agentId);
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
    public Map<String, Object> getParameters(String agentId) {
        String url = difyBaseUrl + "/v1/parameters";

        // 发送请求
        HttpHeaders headers = createHeaders(agentId);
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
    public Map<String, Object> getMeta(String agentId) {
        String url = difyBaseUrl + "/v1/meta";

        HttpHeaders headers = createHeaders(agentId);
        HttpEntity<String> entity = new HttpEntity<>(headers);

        ResponseEntity<Map> response = restTemplate.exchange(
                url,
                HttpMethod.GET,
                entity,
                Map.class
        );
        Map body = response.getBody();
        if (agentId != null) {
            String icon = agentsService.getById(agentId).getIcon();
            body.put("icon", icon);
        } else {
            body.put("icon", "data:image/png;base64,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");
        }

        return body;
    }

    @Override
    public Map<String, Object> uploadFile(String agentId, MultipartFile file, String user) {
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

            HttpHeaders headers = createHeaders(agentId);
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

    /**
     * 根据智能体ID创建请求头
     */
    private HttpHeaders createHeaders(String agentId) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        // 根据agentId获取对应的API Key
        String apiKey = getApiKeyByAgentId(agentId);
        headers.set("Authorization", "Bearer " + apiKey);

        return headers;
    }

    /**
     * 根据智能体ID获取API Key
     */
    private String getApiKeyByAgentId(String agentId) {
        try {
            // 查询智能体信息
            com.diit.ds.domain.entity.Agents agent = agentsService.getById(agentId);
            if (agent != null && agent.getApiKey() != null && !agent.getApiKey().isEmpty()) {
                return agent.getApiKey();
            } else {
                // 如果没有找到对应的API Key，使用默认的
                log.warn("智能体ID {} 未找到对应的API Key，使用默认配置", agentId);
                return difyConfig.getChatAgentToken().replace("Bearer ", "");
            }
        } catch (Exception e) {
            log.error("获取智能体API Key失败，使用默认配置", e);
            return difyConfig.getChatAgentToken().replace("Bearer ", "");
        }
    }

}