package com.diit.ds.controller;

import com.diit.ds.domain.req.DifyKnowledgeReq;
import com.diit.ds.domain.resp.DifyKnowledgeResp;
import com.diit.ds.service.DifyKnowledgeService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.web.bind.annotation.*;

/**
 * Dify外接华三知识库控制器
 */
@Slf4j
@RestController
@RequestMapping("/h3c/api/knowledge")
@Tag(name = "知识库API", description = "Dify外接H3C知识库API")
@CrossOrigin(origins = "*", allowedHeaders = "*", methods = {RequestMethod.GET, RequestMethod.POST, RequestMethod.PUT, RequestMethod.DELETE, RequestMethod.OPTIONS}, allowCredentials = "false")
public class H3C2DifyKnowledgeController {

    private final DifyKnowledgeService difyKnowledgeService;

    public H3C2DifyKnowledgeController(@Qualifier("h3c2DifyKnowledgeServiceImpl") DifyKnowledgeService difyKnowledgeService) {
        this.difyKnowledgeService = difyKnowledgeService;
    }

    /**
     * 从知识库中检索相关内容
     */
    @PostMapping("/retrieval")
    @Operation(summary = "知识库查询", description = "从知识库中检索相关内容")
    public DifyKnowledgeResp retrieveKnowledge(@RequestBody DifyKnowledgeReq req) {
        log.info("接收到知识库查询请求：{}", req);
        return difyKnowledgeService.retrieveKnowledge(req);
    }
} 