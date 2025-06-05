package com.diit.ds.web;

import com.diit.ds.rag.domain.req.AgentKnowledgeHttpReq;
import com.diit.ds.rag.domain.req.DifyKnowledgeHttpReq;
import com.diit.ds.rag.domain.req.DifyKnowledgeReq;
import com.diit.ds.rag.domain.resp.DifyKnowledgeHttpResp;
import com.diit.ds.rag.domain.resp.DifyKnowledgeResp;
import com.diit.ds.rag.service.AgentKdbService;
import com.diit.ds.rag.service.DifyKnowledgeService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Dify外接RAGFlow知识库API控制器
 */
@Slf4j
@RestController
@RequestMapping("/api/ragflow/api/knowledge")
@Tag(name = "知识库知识查询", description = "Dify使用的知识库相关接口")
public class RAGFlow2DifyKnowledgeController {

    private final DifyKnowledgeService difyKnowledgeService;
    private final AgentKdbService agentKdbService;

    public RAGFlow2DifyKnowledgeController(
            @Qualifier("ragFlow2DifyKnowledgeServiceImpl") DifyKnowledgeService difyKnowledgeService,
            AgentKdbService agentKdbService) {
        this.difyKnowledgeService = difyKnowledgeService;
        this.agentKdbService = agentKdbService;
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

    /**
     * 智能体的知识查询接口
     */
    @PostMapping("/retrievalAgent")
    @Operation(summary = "Dify智能体通过Http调用实现的知识查询接口", description = "需要创建Http调用")
    public List<DifyKnowledgeHttpResp.SimpleRecord> retrieveKnowledgeAgent(@RequestBody AgentKnowledgeHttpReq req) {
        log.info("接收到知识库检索请求: {}", req);
        DifyKnowledgeHttpResp resp = agentKdbService.retrieveKnowledgeHttp(req);
        log.info("知识库检索结果数量: {}", resp.getRecords() != null ? resp.getRecords().size() : 0);

        return resp.getRecords();
    }
} 