package com.diit.ds.rag.service.impl;

import com.diit.ds.rag.domain.req.DiosRetrieveReq;
import com.diit.ds.rag.domain.resp.DiosRetrieveResp;
import com.diit.ds.rag.service.DiosAPIService;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.ArrayList;
import java.util.List;

/**
 * DIOS API服务实现类
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class DiosAPIServiceImpl implements DiosAPIService {

    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;
    
    @Value("${diit.dios.api.url:http://wuhan.diit.cn:8000}")
    private String diosApiUrl;

    @Override
    public DiosRetrieveResp retrieve(DiosRetrieveReq retrieveReq) {
        log.info("执行DIOS检索操作，请求参数：{}", retrieveReq);
        
        // 构建请求URL
        String url = diosApiUrl + "/retrieve";
        
        // 构建请求头
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("accept", "application/json");
        
        // 发送请求
        HttpEntity<DiosRetrieveReq> requestEntity = new HttpEntity<>(retrieveReq, headers);
        String responseJson = restTemplate.postForObject(url, requestEntity, String.class);
        
        // 解析响应
        DiosRetrieveResp resp = new DiosRetrieveResp();
        
        try {
            // 将JSON字符串解析为RetrieveResult列表
            List<DiosRetrieveResp.RetrieveResult> retrieveResults = objectMapper.readValue(responseJson,
                    new TypeReference<List<DiosRetrieveResp.RetrieveResult>>() {});
            
            resp.setRetrieveResults(retrieveResults);
            
        } catch (JsonProcessingException e) {
            log.error("解析DIOS检索响应失败", e);
            resp.setRetrieveResults(new ArrayList<>());
        }
        
        log.info("DIOS检索操作完成，返回结果数量：{}", resp.getRetrieveResults().size());
        return resp;
    }

} 