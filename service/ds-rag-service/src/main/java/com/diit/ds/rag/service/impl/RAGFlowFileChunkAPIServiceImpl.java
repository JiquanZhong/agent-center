package com.diit.ds.rag.service.impl;

import com.diit.ds.rag.config.RAGFlowConfig;
import com.diit.ds.rag.domain.req.RagFlowChunkListReq;
import com.diit.ds.rag.domain.req.RagFlowChunkUpdateReq;
import com.diit.ds.rag.domain.resp.RagFlowChunkListResp;
import com.diit.ds.rag.domain.resp.RagFlowChunkUpdateResp;
import com.diit.ds.rag.service.RAGFlowFileChunkAPIService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

/**
 * RAGFlow文件切片API服务实现类
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class RAGFlowFileChunkAPIServiceImpl implements RAGFlowFileChunkAPIService {
    
    private static final String API_PREFIX = "/api/v1";
    private static final String DATASETS_ENDPOINT = "/datasets";
    private static final String DOCUMENTS_ENDPOINT = "/documents";
    private static final String CHUNKS_ENDPOINT = "/chunks";
    
    private final RAGFlowConfig ragFlowConfig;
    private final RestTemplate restTemplate;
    
    @Override
    public RagFlowChunkListResp listChunks(String datasetId, String documentId, RagFlowChunkListReq req) {
        try {
            // 构建带查询参数的URL
            UriComponentsBuilder builder = UriComponentsBuilder.fromHttpUrl(
                    ragFlowConfig.getBaseUrl() + API_PREFIX + DATASETS_ENDPOINT + "/" + datasetId + 
                    DOCUMENTS_ENDPOINT + "/" + documentId + CHUNKS_ENDPOINT);
            
            // 添加查询参数
            if (req.getPage() != null) {
                builder.queryParam("page", req.getPage());
            }
            if (req.getPageSize() != null) {
                builder.queryParam("page_size", req.getPageSize());
            }
            if (req.getKeywords() != null) {
                builder.queryParam("keywords", req.getKeywords());
            }
            if (req.getId() != null) {
                builder.queryParam("id", req.getId());
            }
            
            String url = builder.build().toUriString();
            log.info("RAGFlow 查看切片API请求URL: {}", url);
            
            // 设置请求头
            HttpHeaders headers = new HttpHeaders();
            headers.set("Content-Type", "application/json");
            headers.set("Authorization", ragFlowConfig.getApiKey());
            
            // 创建HTTP请求实体
            HttpEntity<Void> requestEntity = new HttpEntity<>(headers);
            
            // 发送GET请求
            ResponseEntity<RagFlowChunkListResp> responseEntity = restTemplate.exchange(
                    url,
                    HttpMethod.GET,
                    requestEntity,
                    RagFlowChunkListResp.class
            );
            
            // 获取响应结果
            RagFlowChunkListResp response = responseEntity.getBody();
            
            if(response != null && response.getCode() == 0) {
                log.info("RAGFlow 查看切片API成功执行，获取到{}个切片", 
                        response.getData() != null && response.getData().getChunks() != null ? 
                        response.getData().getChunks().size() : 0);
            } else {
                log.error("RAGFlow 查看切片API响应错误: {}", response != null ? response.getMessage() : "null");
            }
            
            return response;
        } catch (Exception e) {
            log.error("调用RAGFlow 查看切片API失败", e);
            // 创建一个错误响应
            RagFlowChunkListResp errorResp = new RagFlowChunkListResp();
            errorResp.setCode(500);
            errorResp.setMessage("调用RAGFlow 查看切片API失败: " + e.getMessage());
            return errorResp;
        }
    }
    
    @Override
    public RagFlowChunkUpdateResp updateChunk(String datasetId, String documentId, String chunkId, RagFlowChunkUpdateReq req) {
        try {
            // 构建请求URL
            String url = ragFlowConfig.getBaseUrl() + API_PREFIX + DATASETS_ENDPOINT + "/" + datasetId + 
                    DOCUMENTS_ENDPOINT + "/" + documentId + CHUNKS_ENDPOINT + "/" + chunkId;
            log.info("RAGFlow 更新切片API请求URL: {}", url);
            
            // 设置请求头
            HttpHeaders headers = new HttpHeaders();
            headers.set("Content-Type", "application/json");
            headers.set("Authorization", ragFlowConfig.getApiKey());
            
            // 创建HTTP请求实体
            HttpEntity<RagFlowChunkUpdateReq> requestEntity = new HttpEntity<>(req, headers);
            
            // 发送PUT请求
            ResponseEntity<RagFlowChunkUpdateResp> responseEntity = restTemplate.exchange(
                    url,
                    HttpMethod.PUT,
                    requestEntity,
                    RagFlowChunkUpdateResp.class
            );
            
            // 获取响应结果
            RagFlowChunkUpdateResp response = responseEntity.getBody();
            
            if(response != null && response.getCode() == 0) {
                log.info("RAGFlow 更新切片API成功执行");
            } else {
                log.error("RAGFlow 更新切片API响应错误: {}", response != null ? response.getMessage() : "null");
            }
            
            return response;
        } catch (Exception e) {
            log.error("调用RAGFlow 更新切片API失败", e);
            // 创建一个错误响应
            RagFlowChunkUpdateResp errorResp = new RagFlowChunkUpdateResp();
            errorResp.setCode(500);
            errorResp.setMessage("调用RAGFlow 更新切片API失败: " + e.getMessage());
            return errorResp;
        }
    }
}
