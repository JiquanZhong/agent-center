package com.diit.ds.controller;

import com.diit.ds.domain.req.DifyKnowledgeReq;
import com.diit.ds.domain.resp.DifyKnowledgeResp;
import com.diit.ds.service.DifyKnowledgeService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Dify外部知识库API控制器
 */
@Slf4j
@RestController
@RequestMapping("/api/knowledge")
@RequiredArgsConstructor
public class DifyKnowledgeController {

    private final DifyKnowledgeService difyKnowledgeService;

    /**
     * 从知识库中检索相关内容
     * 
     * @param req 检索请求
     * @return 检索结果
     */
    @PostMapping("/retrieval")
    public DifyKnowledgeResp retrieveKnowledge(@RequestBody DifyKnowledgeReq req) {
        log.info("接收到知识库检索请求: {}", req);
        DifyKnowledgeResp resp = difyKnowledgeService.retrieveKnowledge(req);
        log.info("知识库检索结果数量: {}", resp.getRecords() != null ? resp.getRecords().size() : 0);
        return resp;
    }
} 