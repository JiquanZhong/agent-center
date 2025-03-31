package com.diit.ds.service.impl;

import com.diit.ds.config.RAGFlowConfig;
import com.diit.ds.domain.req.RAGFlowDatasetCreateReq;
import com.diit.ds.domain.req.RAGFlowDatasetDeleteReq;
import com.diit.ds.domain.req.RAGFlowDatasetListReq;
import com.diit.ds.domain.req.RAGFlowDatasetUpdateReq;
import com.diit.ds.domain.req.RAGFlowKnowledgeReq;
import com.diit.ds.domain.resp.RAGFlowDatasetCreateResp;
import com.diit.ds.domain.resp.RAGFlowDatasetDeleteResp;
import com.diit.ds.domain.resp.RAGFlowDatasetListResp;
import com.diit.ds.domain.resp.RAGFlowDatasetUpdateResp;
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
import org.springframework.web.util.UriComponentsBuilder;

import java.util.List;

/**
 * RAGFlowAPI调用实现类
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class RAGFlowAPIServiceImpl implements RAGFlowAPIService {
    private static final String API_PREFIX = "/api/v1";
    private static final String RETRIEVAL_ENDPOINT = "/retrieval";
    private static final String DATASETS_ENDPOINT = "/datasets";

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

    @Override
    public RAGFlowDatasetCreateResp createDataset(RAGFlowDatasetCreateReq req) {
        try {
            // 构建请求URL
            String url = ragFlowConfig.getBaseUrl() + API_PREFIX + DATASETS_ENDPOINT;
            log.info("RAGFlow 创建数据集API请求URL: {}", url);
            
            // 设置请求头
            HttpHeaders headers = new HttpHeaders();
            headers.set("Content-Type", "application/json");
            headers.set("Authorization", ragFlowConfig.getApiKey());
            
            // 创建HTTP请求实体
            HttpEntity<RAGFlowDatasetCreateReq> requestEntity = new HttpEntity<>(req, headers);
            
            // 发送POST请求
            ResponseEntity<RAGFlowDatasetCreateResp> responseEntity = restTemplate.exchange(
                    url,
                    HttpMethod.POST,
                    requestEntity,
                    RAGFlowDatasetCreateResp.class
            );
            
            // 获取响应结果
            RAGFlowDatasetCreateResp response = responseEntity.getBody();

            if(response != null && response.getCode() == 0) {
                log.info("RAGFlow 创建数据集API响应数据集: {}", response.getData());
            } else {
                log.error("RAGFlow 创建数据集API响应错误: {}", response != null ? response.getMessage() : "null");
            }

            return response;
        } catch (Exception e) {
            log.error("调用RAGFlow 创建数据集API失败", e);
            // 创建一个错误响应
            RAGFlowDatasetCreateResp errorResp = new RAGFlowDatasetCreateResp();
            errorResp.setCode(500);
            errorResp.setMessage("调用RAGFlow 创建数据集API失败: " + e.getMessage());
            return errorResp;
        }
    }

    @Override
    public RAGFlowDatasetDeleteResp deleteDatasets(RAGFlowDatasetDeleteReq req) {
        try {
            // 构建请求URL
            String url = ragFlowConfig.getBaseUrl() + API_PREFIX + DATASETS_ENDPOINT;
            log.info("RAGFlow 删除数据集API请求URL: {}", url);
            
            // 设置请求头
            HttpHeaders headers = new HttpHeaders();
            headers.set("Content-Type", "application/json");
            headers.set("Authorization", ragFlowConfig.getApiKey());
            
            // 创建HTTP请求实体
            HttpEntity<RAGFlowDatasetDeleteReq> requestEntity = new HttpEntity<>(req, headers);
            
            // 发送DELETE请求
            ResponseEntity<RAGFlowDatasetDeleteResp> responseEntity = restTemplate.exchange(
                    url,
                    HttpMethod.DELETE,
                    requestEntity,
                    RAGFlowDatasetDeleteResp.class
            );
            
            // 获取响应结果
            RAGFlowDatasetDeleteResp response = responseEntity.getBody();

            if(response != null && response.getCode() == 0) {
                log.info("RAGFlow 删除数据集API成功执行");
            } else {
                log.error("RAGFlow 删除数据集API响应错误: {}", response != null ? response.getMessage() : "null");
            }
            
            return response;
        } catch (Exception e) {
            log.error("调用RAGFlow 删除数据集API失败", e);
            // 创建一个错误响应
            RAGFlowDatasetDeleteResp errorResp = new RAGFlowDatasetDeleteResp();
            errorResp.setCode(500);
            errorResp.setMessage("调用RAGFlow 删除数据集API失败: " + e.getMessage());
            return errorResp;
        }
    }

    @Override
    public RAGFlowDatasetUpdateResp updateDataset(String datasetId, RAGFlowDatasetUpdateReq req) {
        try {
            // 构建请求URL
            String url = ragFlowConfig.getBaseUrl() + API_PREFIX + DATASETS_ENDPOINT + "/" + datasetId;
            log.info("RAGFlow 更新数据集API请求URL: {}", url);
            
            // 设置请求头
            HttpHeaders headers = new HttpHeaders();
            headers.set("Content-Type", "application/json");
            headers.set("Authorization", ragFlowConfig.getApiKey());
            
            // 创建HTTP请求实体
            HttpEntity<RAGFlowDatasetUpdateReq> requestEntity = new HttpEntity<>(req, headers);
            
            // 发送PUT请求
            ResponseEntity<RAGFlowDatasetUpdateResp> responseEntity = restTemplate.exchange(
                    url,
                    HttpMethod.PUT,
                    requestEntity,
                    RAGFlowDatasetUpdateResp.class
            );
            
            // 获取响应结果
            RAGFlowDatasetUpdateResp response = responseEntity.getBody();

            if(response != null && response.getCode() == 0) {
                log.info("RAGFlow 更新数据集API成功执行");
            } else {
                log.error("RAGFlow 更新数据集API响应错误: {}", response != null ? response.getMessage() : "null");
            }
            
            return response;
        } catch (Exception e) {
            log.error("调用RAGFlow 更新数据集API失败", e);
            // 创建一个错误响应
            RAGFlowDatasetUpdateResp errorResp = new RAGFlowDatasetUpdateResp();
            errorResp.setCode(500);
            errorResp.setMessage("调用RAGFlow 更新数据集API失败: " + e.getMessage());
            return errorResp;
        }
    }

    @Override
    public RAGFlowDatasetListResp listDatasets(RAGFlowDatasetListReq req) {
        try {
            // 构建带查询参数的URL
            UriComponentsBuilder builder = UriComponentsBuilder.fromHttpUrl(
                    ragFlowConfig.getBaseUrl() + API_PREFIX + DATASETS_ENDPOINT);
            
            // 添加查询参数
            if (req.getPage() != null) {
                builder.queryParam("page", req.getPage());
            }
            if (req.getPageSize() != null) {
                builder.queryParam("page_size", req.getPageSize());
            }
            if (req.getOrderby() != null) {
                builder.queryParam("orderby", req.getOrderby());
            }
            if (req.getDesc() != null) {
                builder.queryParam("desc", req.getDesc());
            }
            if (req.getName() != null) {
                builder.queryParam("name", req.getName());
            }
            if (req.getId() != null) {
                builder.queryParam("id", req.getId());
            }
            
            String url = builder.build().toUriString();
            log.info("RAGFlow 获取数据集列表API请求URL: {}", url);
            
            // 设置请求头
            HttpHeaders headers = new HttpHeaders();
            headers.set("Content-Type", "application/json");
            headers.set("Authorization", ragFlowConfig.getApiKey());
            
            // 创建HTTP请求实体
            HttpEntity<Void> requestEntity = new HttpEntity<>(headers);
            
            // 发送GET请求
            ResponseEntity<RAGFlowDatasetListResp> responseEntity = restTemplate.exchange(
                    url,
                    HttpMethod.GET,
                    requestEntity,
                    RAGFlowDatasetListResp.class
            );
            
            // 获取响应结果
            RAGFlowDatasetListResp response = responseEntity.getBody();

            if(response != null && response.getCode() == 0) {
                log.info("RAGFlow 获取数据集列表API成功执行，获取到{}个数据集", 
                        response.getData() != null ? response.getData().size() : 0);
            } else {
                log.error("RAGFlow 获取数据集列表API响应错误: {}", response != null ? response.getMessage() : "null");
            }
            
            return response;
        } catch (Exception e) {
            log.error("调用RAGFlow 获取数据集列表API失败", e);
            // 创建一个错误响应
            RAGFlowDatasetListResp errorResp = new RAGFlowDatasetListResp();
            errorResp.setCode(500);
            errorResp.setMessage("调用RAGFlow 获取数据集列表API失败: " + e.getMessage());
            return errorResp;
        }
    }
}
