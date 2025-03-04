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

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
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
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("Authorization", difyConfig.getChatAgentToken());

        HttpEntity<Map<String, Object>> entity = new HttpEntity<>(requestBody, headers);

        ResponseEntity<Map> response = restTemplate.exchange(
                difyBaseUrl + "/v1/chat-messages",
                HttpMethod.POST,
                entity,
                Map.class
        );
        log.info("Response from Dify API: {}", response.getBody());

        return response.getBody();
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
}