package com.diit.ds.web;

import com.diit.ds.domain.req.DifyKnowledgeHttpReq;
import com.diit.ds.domain.req.DifyKnowledgeReq;
import com.diit.ds.domain.resp.DifyKnowledgeHttpResp;
import com.diit.ds.domain.resp.DifyKnowledgeResp;
import com.diit.ds.service.DifyKnowledgeService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * Dify外接DIOS知识库API控制器
 */
@Slf4j
@RestController
@RequestMapping("/api/dios/api/knowledge")
@Tag(name = "DIOS知识库知识查询", description = "DIOS使用的知识库相关接口")
public class Dios2DifyKnowledgeController {

    private final DifyKnowledgeService difyKnowledgeService;

    public Dios2DifyKnowledgeController(@Qualifier("dios2DifyKnowledgeServiceImpl") DifyKnowledgeService difyKnowledgeService) {
        this.difyKnowledgeService = difyKnowledgeService;
    }

    /**
     * 从知识库中检索相关内容
     *
     * @param req 检索请求
     * @return 检索结果
     */
    @PostMapping("/retrieval")
    @Operation(summary = "适配Dify外部知识库的接口", description = "直接适配Dify外部知识库的接口")
    public DifyKnowledgeResp retrieveKnowledge(@RequestBody DifyKnowledgeReq req) {
        log.info("接收到知识库检索请求: {}", req);
        DifyKnowledgeResp resp = difyKnowledgeService.retrieveKnowledge(req);
        log.info("知识库检索结果数量: {}", resp.getRecords() != null ? resp.getRecords().size() : 0);
        return resp;
    }

    /**
     * 从知识库中检索相关内容
     *
     * @param req 检索请求
     * @return 检索结果
     */
    @PostMapping("/retrievalSimple")
    @Operation(summary = "通过Http调用实现的知识查询接口", description = "需要创建Http调用")
    public List<DifyKnowledgeHttpResp.SimpleRecord> retrieveKnowledgeSimple(@RequestBody DifyKnowledgeHttpReq req) {
        log.info("接收到知识库检索请求: {}", req);
        DifyKnowledgeHttpResp resp = difyKnowledgeService.retrieveKnowledgeHttp(req);
        log.info("知识库检索结果数量: {}", resp.getRecords() != null ? resp.getRecords().size() : 0);

        return resp.getRecords();
    }
} 