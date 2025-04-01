package com.diit.ds.service.impl;

import com.diit.ds.config.RAGFlowConfig;
import com.diit.ds.domain.req.RAGFlowFileDeleteReq;
import com.diit.ds.domain.req.RAGFlowFileListReq;
import com.diit.ds.domain.req.RAGFlowFileParseReq;
import com.diit.ds.domain.resp.RAGFlowFileDeleteResp;
import com.diit.ds.domain.resp.RAGFlowFileListResp;
import com.diit.ds.domain.resp.RAGFlowFileParseResp;
import com.diit.ds.domain.resp.RAGFlowFileUploadResp;
import com.diit.ds.service.RAGFlowFileAPIService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.util.UriComponentsBuilder;

import java.io.IOException;
import java.io.OutputStream;

/**
 * RAGFlow文件API服务实现类
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class RAGFlowFileAPIServiceImpl implements RAGFlowFileAPIService {
    private static final String API_PREFIX = "/api/v1";
    private static final String DATASETS_ENDPOINT = "/datasets";
    private static final String DOCUMENTS_ENDPOINT = "/documents";
    private static final String CHUNKS_ENDPOINT = "/chunks";

    private final RAGFlowConfig ragFlowConfig;
    private final RestTemplate restTemplate;

    @Override
    public RAGFlowFileUploadResp uploadFiles(String datasetId, MultipartFile[] files) {
        try {
            // 构建请求URL
            String url = ragFlowConfig.getBaseUrl() + API_PREFIX + DATASETS_ENDPOINT + "/" + datasetId + DOCUMENTS_ENDPOINT;
            log.info("RAGFlow 上传文件API请求URL: {}", url);

            // 设置请求头
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.MULTIPART_FORM_DATA);
            headers.set("Authorization", ragFlowConfig.getApiKey());

            // 构建表单数据
            MultiValueMap<String, Object> formData = new LinkedMultiValueMap<>();
            for (MultipartFile file : files) {
                try {
                    // 将每个文件添加到表单数据中
                    ByteArrayResource resource = new ByteArrayResource(file.getBytes()) {
                        @Override
                        public String getFilename() {
                            return file.getOriginalFilename();
                        }
                    };
                    formData.add("file", resource);
                } catch (IOException e) {
                    log.error("处理上传文件时发生错误", e);
                    RAGFlowFileUploadResp errorResp = new RAGFlowFileUploadResp();
                    errorResp.setCode(500);
                    errorResp.setMessage("处理上传文件时发生错误: " + e.getMessage());
                    return errorResp;
                }
            }

            // 创建HTTP请求实体
            HttpEntity<MultiValueMap<String, Object>> requestEntity = new HttpEntity<>(formData, headers);

            // 发送POST请求
            ResponseEntity<RAGFlowFileUploadResp> responseEntity = restTemplate.exchange(
                    url,
                    HttpMethod.POST,
                    requestEntity,
                    RAGFlowFileUploadResp.class
            );

            // 获取响应结果
            RAGFlowFileUploadResp response = responseEntity.getBody();
            log.info("RAGFlow 上传文件API响应状态码: {}", response != null ? response.getCode() : "null");

            return response;
        } catch (Exception e) {
            log.error("调用RAGFlow 上传文件API失败", e);
            // 创建一个错误响应
            RAGFlowFileUploadResp errorResp = new RAGFlowFileUploadResp();
            errorResp.setCode(500);
            errorResp.setMessage("调用RAGFlow 上传文件API失败: " + e.getMessage());
            return errorResp;
        }
    }

    @Override
    public boolean downloadFile(String datasetId, String documentId, OutputStream outputStream) {
        try {
            // 构建请求URL
            String url = ragFlowConfig.getBaseUrl() + API_PREFIX + DATASETS_ENDPOINT + "/" + datasetId + DOCUMENTS_ENDPOINT + "/" + documentId;
            log.info("RAGFlow 下载文件API请求URL: {}", url);

            // 设置请求头
            HttpHeaders headers = new HttpHeaders();
            headers.set("Authorization", ragFlowConfig.getApiKey());

            // 创建HTTP请求实体
            HttpEntity<Void> requestEntity = new HttpEntity<>(headers);

            // 发送GET请求并直接获取响应实体
            ResponseEntity<byte[]> responseEntity = restTemplate.exchange(
                    url,
                    HttpMethod.GET,
                    requestEntity,
                    byte[].class
            );

            // 将响应体写入输出流
            if (responseEntity.getBody() != null) {
                outputStream.write(responseEntity.getBody());
                outputStream.flush();
                return true;
            } else {
                log.error("下载文件失败，响应体为空");
                return false;
            }
        } catch (Exception e) {
            log.error("调用RAGFlow 下载文件API失败", e);
            return false;
        }
    }

    @Override
    public RAGFlowFileListResp listFiles(String datasetId, RAGFlowFileListReq req) {
        try {
            // 构建带查询参数的URL
            UriComponentsBuilder builder = UriComponentsBuilder.fromHttpUrl(
                    ragFlowConfig.getBaseUrl() + API_PREFIX + DATASETS_ENDPOINT + "/" + datasetId + DOCUMENTS_ENDPOINT);
            
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
            if (req.getKeywords() != null) {
                builder.queryParam("keywords", req.getKeywords());
            }
            if (req.getId() != null) {
                builder.queryParam("id", req.getId());
            }
            if (req.getName() != null) {
                builder.queryParam("name", req.getName());
            }
            
            String url = builder.build().toUriString();
            log.info("RAGFlow 文件列表API请求URL: {}", url);
            
            // 设置请求头
            HttpHeaders headers = new HttpHeaders();
            headers.set("Content-Type", "application/json");
            headers.set("Authorization", ragFlowConfig.getApiKey());
            
            // 创建HTTP请求实体
            HttpEntity<Void> requestEntity = new HttpEntity<>(headers);
            
            // 发送GET请求
            ResponseEntity<RAGFlowFileListResp> responseEntity = restTemplate.exchange(
                    url,
                    HttpMethod.GET,
                    requestEntity,
                    RAGFlowFileListResp.class
            );
            
            // 获取响应结果
            RAGFlowFileListResp response = responseEntity.getBody();

            if(response != null && response.getCode() == 0) {
                log.info("RAGFlow 文件列表API成功执行，获取到{}个文件", 
                        response.getData() != null && response.getData().getDocs() != null ? response.getData().getDocs().size() : 0);
            } else {
                log.error("RAGFlow 文件列表API响应错误: {}", response != null ? response.getMessage() : "null");
            }
            
            return response;
        } catch (Exception e) {
            log.error("调用RAGFlow 文件列表API失败", e);
            // 创建一个错误响应
            RAGFlowFileListResp errorResp = new RAGFlowFileListResp();
            errorResp.setCode(500);
            errorResp.setMessage("调用RAGFlow 文件列表API失败: " + e.getMessage());
            return errorResp;
        }
    }

    @Override
    public RAGFlowFileDeleteResp deleteFiles(String datasetId, RAGFlowFileDeleteReq req) {
        try {
            // 构建请求URL
            String url = ragFlowConfig.getBaseUrl() + API_PREFIX + DATASETS_ENDPOINT + "/" + datasetId + DOCUMENTS_ENDPOINT;
            log.info("RAGFlow 删除文件API请求URL: {}", url);
            
            // 设置请求头
            HttpHeaders headers = new HttpHeaders();
            headers.set("Content-Type", "application/json");
            headers.set("Authorization", ragFlowConfig.getApiKey());
            
            // 创建HTTP请求实体
            HttpEntity<RAGFlowFileDeleteReq> requestEntity = new HttpEntity<>(req, headers);
            
            // 发送DELETE请求
            ResponseEntity<RAGFlowFileDeleteResp> responseEntity = restTemplate.exchange(
                    url,
                    HttpMethod.DELETE,
                    requestEntity,
                    RAGFlowFileDeleteResp.class
            );
            
            // 获取响应结果
            RAGFlowFileDeleteResp response = responseEntity.getBody();

            if(response != null && response.getCode() == 0) {
                log.info("RAGFlow 删除文件API成功执行");
            } else {
                log.error("RAGFlow 删除文件API响应错误: {}", response != null ? response.getMessage() : "null");
            }
            
            return response;
        } catch (Exception e) {
            log.error("调用RAGFlow 删除文件API失败", e);
            // 创建一个错误响应
            RAGFlowFileDeleteResp errorResp = new RAGFlowFileDeleteResp();
            errorResp.setCode(500);
            errorResp.setMessage("调用RAGFlow 删除文件API失败: " + e.getMessage());
            return errorResp;
        }
    }

    @Override
    public RAGFlowFileParseResp startParseTask(String datasetId, RAGFlowFileParseReq req) {
        try {
            // 构建请求URL
            String url = ragFlowConfig.getBaseUrl() + API_PREFIX + DATASETS_ENDPOINT + "/" + datasetId + CHUNKS_ENDPOINT;
            log.info("RAGFlow 开始解析文件API请求URL: {}", url);
            
            // 设置请求头
            HttpHeaders headers = new HttpHeaders();
            headers.set("Content-Type", "application/json");
            headers.set("Authorization", ragFlowConfig.getApiKey());
            
            // 创建HTTP请求实体
            HttpEntity<RAGFlowFileParseReq> requestEntity = new HttpEntity<>(req, headers);
            
            // 发送POST请求
            ResponseEntity<RAGFlowFileParseResp> responseEntity = restTemplate.exchange(
                    url,
                    HttpMethod.POST,
                    requestEntity,
                    RAGFlowFileParseResp.class
            );
            
            // 获取响应结果
            RAGFlowFileParseResp response = responseEntity.getBody();

            if(response != null && response.getCode() == 0) {
                log.info("RAGFlow 开始解析文件API成功执行");
            } else {
                log.error("RAGFlow 开始解析文件API响应错误: {}", response != null ? response.getMessage() : "null");
            }
            
            return response;
        } catch (Exception e) {
            log.error("调用RAGFlow 开始解析文件API失败", e);
            // 创建一个错误响应
            RAGFlowFileParseResp errorResp = new RAGFlowFileParseResp();
            errorResp.setCode(500);
            errorResp.setMessage("调用RAGFlow 开始解析文件API失败: " + e.getMessage());
            return errorResp;
        }
    }

    @Override
    public RAGFlowFileParseResp stopParseTask(String datasetId, RAGFlowFileParseReq req) {
        try {
            // 构建请求URL
            String url = ragFlowConfig.getBaseUrl() + API_PREFIX + DATASETS_ENDPOINT + "/" + datasetId + CHUNKS_ENDPOINT;
            log.info("RAGFlow 停止解析文件API请求URL: {}", url);
            
            // 设置请求头
            HttpHeaders headers = new HttpHeaders();
            headers.set("Content-Type", "application/json");
            headers.set("Authorization", ragFlowConfig.getApiKey());
            
            // 创建HTTP请求实体
            HttpEntity<RAGFlowFileParseReq> requestEntity = new HttpEntity<>(req, headers);
            
            // 发送DELETE请求
            ResponseEntity<RAGFlowFileParseResp> responseEntity = restTemplate.exchange(
                    url,
                    HttpMethod.DELETE,
                    requestEntity,
                    RAGFlowFileParseResp.class
            );
            
            // 获取响应结果
            RAGFlowFileParseResp response = responseEntity.getBody();

            if(response != null && response.getCode() == 0) {
                log.info("RAGFlow 停止解析文件API成功执行");
            } else {
                log.error("RAGFlow 停止解析文件API响应错误: {}", response != null ? response.getMessage() : "null");
            }
            
            return response;
        } catch (Exception e) {
            log.error("调用RAGFlow 停止解析文件API失败", e);
            // 创建一个错误响应
            RAGFlowFileParseResp errorResp = new RAGFlowFileParseResp();
            errorResp.setCode(500);
            errorResp.setMessage("调用RAGFlow 停止解析文件API失败: " + e.getMessage());
            return errorResp;
        }
    }
}
