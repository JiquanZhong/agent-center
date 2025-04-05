package com.diit.ds.service.impl;

import com.diit.ds.config.H3CKnowledgeConfig;
import com.diit.ds.domain.req.DifyKnowledgeHttpReq;
import com.diit.ds.domain.req.DifyKnowledgeReq;
import com.diit.ds.domain.req.H3CAuthReq;
import com.diit.ds.domain.req.H3CKnowledgeReq;
import com.diit.ds.domain.resp.DifyKnowledgeHttpResp;
import com.diit.ds.domain.resp.DifyKnowledgeResp;
import com.diit.ds.domain.resp.H3CKnowledgeResp;
import com.diit.ds.service.DifyKnowledgeService;
import com.diit.ds.util.H3CUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * H3C知识库服务实现
 */
@Slf4j
@Service("h3c2DifyKnowledgeServiceImpl")
@RequiredArgsConstructor
public class H3C2DifyKnowledgeServiceImpl implements DifyKnowledgeService {

    private final RestTemplate restTemplate;
    private final H3CKnowledgeConfig h3cConfig;
    private final H3CUtil h3CUtil;

    @Override
    public DifyKnowledgeResp retrieveKnowledge(DifyKnowledgeReq req) {
        log.info("开始调用H3C知识库查询接口，请求参数：{}", req);
        
        try {
            // 将Dify请求转换为H3C请求
            H3CKnowledgeReq h3cReq = buildH3CKnowledgeReq(req);
            log.debug("转换后的H3C请求参数: {}", h3cReq);
            
            // 设置请求头
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.set("access_token", h3CUtil.getAccessToken());
            log.debug("H3C请求headers设置: content-type={}, access_token={}", 
                    MediaType.APPLICATION_JSON, h3cConfig.getAccessToken());
            
            // 发送请求
            String url = h3cConfig.getBaseUrl() + "/api/hub/open/v1/knowledge/questionSearch";
            log.debug("准备调用H3C知识库API，URL: {}", url);
            HttpEntity<H3CKnowledgeReq> requestEntity = new HttpEntity<>(h3cReq, headers);
            
            // 调用H3C知识库API
            log.info("开始发送请求到H3C知识库...");
            H3CKnowledgeResp h3cResp = restTemplate.postForObject(url, requestEntity, H3CKnowledgeResp.class);
            log.info("H3C知识库查询接口返回状态码: {}, 消息: {}", h3cResp.getCode(), h3cResp.getMsg());
            log.debug("H3C知识库查询接口完整返回: {}", h3cResp);
            
            // 将H3C响应转换为Dify响应
            DifyKnowledgeResp difyResp = buildDifyKnowledgeResp(h3cResp);
            log.info("转换后的Dify响应记录数: {}", difyResp.getRecords() != null ? difyResp.getRecords().size() : 0);
            log.debug("完整的Dify响应内容: {}", difyResp);
            
            return difyResp;
        } catch (Exception e) {
            log.error("调用H3C知识库查询接口异常: {}", e.getMessage(), e);
            // 返回空响应
            return new DifyKnowledgeResp();
        }
    }

    @Override
    public DifyKnowledgeHttpResp retrieveKnowledgeHttp(DifyKnowledgeHttpReq req) {
        return null;
    }

    /**
     * 构建H3C知识库请求
     */
    private H3CKnowledgeReq buildH3CKnowledgeReq(DifyKnowledgeReq req) {
        log.debug("开始构建H3C知识库请求...");
        H3CKnowledgeReq h3cReq = new H3CKnowledgeReq();
        
        // 设置知识库代码列表

        log.debug("使用默认知识库代码: {}", h3cConfig.getDefaultKnowledgeCode().get(0));
        h3cReq.setKnowledgeCodeList(h3cConfig.getDefaultKnowledgeCode());
        
        // 设置源问题和问题列表
        log.debug("设置源问题: {}", req.getQuery());
        h3cReq.setSourceQuestion(req.getQuery());
        h3cReq.setQuestionList(Collections.emptyList());
        
        // 设置问题类型
        log.debug("使用问题类型: {}", h3cConfig.getQuestionType());
        h3cReq.setQuestionType(h3cConfig.getQuestionType());
        
        // 设置返回结果数量
        Integer topK = req.getRetrievalSetting() != null ? req.getRetrievalSetting().getTopK() : 5;
        log.debug("设置topK值: {}", topK);
        h3cReq.setTopK(topK);
        
        log.debug("H3C知识库请求构建完成: {}", h3cReq);
        return h3cReq;
    }
    
    /**
     * 构建Dify知识库响应
     */
    private DifyKnowledgeResp buildDifyKnowledgeResp(H3CKnowledgeResp h3cResp) {
        log.debug("开始构建Dify知识库响应...");
        
        if (h3cResp == null) {
            log.warn("H3C响应为null，返回空的Dify响应");
            return new DifyKnowledgeResp();
        }
        
        if (h3cResp.getData() == null || h3cResp.getData().isEmpty()) {
            log.info("H3C响应数据为空，返回空的Dify响应");
            return new DifyKnowledgeResp();
        }
        
        log.info("H3C响应包含 {} 条结果数据", h3cResp.getData().size());
        
        DifyKnowledgeResp difyResp = new DifyKnowledgeResp();
        List<DifyKnowledgeResp.Record> records = new ArrayList<>();
        
        // 转换知识条目数据
        for (H3CKnowledgeResp.ResultData resultData : h3cResp.getData()) {
            DifyKnowledgeResp.Record record = new DifyKnowledgeResp.Record();
            record.setContent(resultData.getContent());
            record.setScore(resultData.getScore());
            record.setTitle(resultData.getFileName());
            log.debug("添加记录: title={}, score={}", resultData.getFileName(), resultData.getScore());
            records.add(record);
        }
        
        difyResp.setRecords(records);
        log.debug("Dify知识库响应构建完成，共 {} 条记录", records.size());
        return difyResp;
    }
} 