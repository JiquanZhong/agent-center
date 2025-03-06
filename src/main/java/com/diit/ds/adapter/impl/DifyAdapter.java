package com.diit.ds.adapter.impl;

import com.diit.ds.adapter.LLMAdapter;
import com.diit.ds.config.DifyConfig;
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

            return response.getBody();
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
            log.error("获取应用元数据失败", e);
            throw new RuntimeException("获取应用元数据失败: " + e.getMessage());
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