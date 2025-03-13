package com.diit.ds.service.impl;

import com.diit.ds.config.RAGFlowConfig;
import com.diit.ds.domain.req.RAGFlowKnowledgeReq;
import com.diit.ds.domain.resp.RAGFlowKnowledgeResp;
import com.diit.ds.service.RAGFlowAPIService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

/**
 * RAGFlowAPI调用实现类
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class RAGFlowAPIServiceImpl implements RAGFlowAPIService {
    private static final String API_PREFIX = "/api/v1";
    private static final String RETRIEVAL_ENDPOINT = "/retrieval";

    private final RAGFlowConfig ragFlowConfig;
    private final RestTemplate restTemplate;

    @Override
    public RAGFlowKnowledgeResp retrieval(RAGFlowKnowledgeReq req) {
        try {
            // 构建请求URL
            String url = ragFlowConfig.getBaseUrl() + API_PREFIX + RETRIEVAL_ENDPOINT;
            log.info("RAGFlow API请求URL: {}", url);
            
            // 设置请求头
            HttpHeaders headers = new HttpHeaders();
            headers.set("Content-Type", "application/json");
            headers.set("Authorization", ragFlowConfig.getApiKey());
            
            // 创建HTTP请求实体
            HttpEntity<RAGFlowKnowledgeReq> requestEntity = new HttpEntity<>(req, headers);
            
            // 发送POST请求
            ResponseEntity<RAGFlowKnowledgeResp> responseEntity = restTemplate.exchange(
                    url,
                    HttpMethod.POST,
                    requestEntity,
                    RAGFlowKnowledgeResp.class
            );
            
            // 获取响应结果
            RAGFlowKnowledgeResp response = responseEntity.getBody();
            log.info("RAGFlow API响应状态码: {}", response != null ? response.getCode() : "null");
            
            return response;
        } catch (Exception e) {
            log.error("调用RAGFlow API失败", e);
            // 创建一个错误响应
            RAGFlowKnowledgeResp errorResp = new RAGFlowKnowledgeResp();
            errorResp.setCode(500);
            errorResp.setMessage("调用RAGFlow API失败: " + e.getMessage());
            return errorResp;
        }
    }
}
