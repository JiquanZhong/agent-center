package com.diit.ds.service.impl;

import com.diit.ds.domain.entity.KnowledgeTreeNode;
import com.diit.ds.domain.req.DifyKnowledgeReq;
import com.diit.ds.domain.req.RAGFlowKnowledgeReq;
import com.diit.ds.domain.resp.DifyKnowledgeResp;
import com.diit.ds.domain.resp.RAGFlowKnowledgeResp;
import com.diit.ds.service.DifyKnowledgeService;
import com.diit.ds.service.KnowledgeTreeNodeService;
import com.diit.ds.service.RAGFlowDBAPIService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Dify外部知识库API服务实现类
 * 将RAGFlow API适配为Dify外部知识库API
 */
@Slf4j
@Service("ragFlow2DifyKnowledgeServiceImpl")
@RequiredArgsConstructor
public class RAGFlow2DifyKnowledgeServiceImpl implements DifyKnowledgeService {

    private final RAGFlowDBAPIService ragFlowDBAPIService;
    private final KnowledgeTreeNodeService knowledgeTreeNodeService;

    @Override
    public DifyKnowledgeResp retrieveKnowledge(DifyKnowledgeReq req) {
        try {
            // 将Dify请求转换为RAGFlow请求
            RAGFlowKnowledgeReq ragFlowReq = convertToRAGFlowRequest(req);

            // 调用RAGFlow API
            RAGFlowKnowledgeResp ragFlowResp = ragFlowDBAPIService.retrieval(ragFlowReq);

            // 将RAGFlow响应转换为Dify响应
            return convertToDifyResponse(ragFlowResp);
        } catch (Exception e) {
            log.error("调用RAGFlow API失败", e);
            // 返回空结果
            return new DifyKnowledgeResp();
        }
    }

    /**
     * 将Dify请求转换为RAGFlow请求
     */
    private RAGFlowKnowledgeReq convertToRAGFlowRequest(DifyKnowledgeReq req) {
        RAGFlowKnowledgeReq ragFlowReq = new RAGFlowKnowledgeReq();

        // 设置查询
        ragFlowReq.setQuestion(req.getQuery());

        // 设置知识库ID
        // 暴力查全库 以后再优化
        List<KnowledgeTreeNode> allNodes = knowledgeTreeNodeService.getNodesByPid("0");
        List<String> dbIds = allNodes.stream().map(KnowledgeTreeNode::getKdbId).toList();
        ragFlowReq.setDatasetIds(dbIds);

        // 设置检索参数
        if (req.getRetrievalSetting() != null) {
            if (req.getRetrievalSetting().getTopK() != null) {
                ragFlowReq.setTopK(req.getRetrievalSetting().getTopK());
            }

            if (req.getRetrievalSetting().getScoreThreshold() != null) {
                ragFlowReq.setSimilarityThreshold(req.getRetrievalSetting().getScoreThreshold());
            }
        }

        ragFlowReq.setPage(1);
        ragFlowReq.setPageSize(5);
        ragFlowReq.setVectorSimilarityWeight(1.0);

        return ragFlowReq;
    }

    /**
     * 将RAGFlow响应转换为Dify响应
     */
    private DifyKnowledgeResp convertToDifyResponse(RAGFlowKnowledgeResp ragFlowResp) {
        DifyKnowledgeResp difyResp = new DifyKnowledgeResp();

        if (ragFlowResp == null || ragFlowResp.getData() == null || ragFlowResp.getData().getChunks() == null) {
            difyResp.setRecords(Collections.emptyList());
            return difyResp;
        }

        List<DifyKnowledgeResp.Record> records = new ArrayList<>();

        for (RAGFlowKnowledgeResp.RespData.Chunk chunk : ragFlowResp.getData().getChunks()) {
            DifyKnowledgeResp.Record record = new DifyKnowledgeResp.Record();

            // 设置文档内容
            record.setContent(chunk.getContent());

            // 设置相关性分数
            record.setScore(chunk.getSimilarity());

            // 设置文档标题
            record.setTitle(chunk.getDocumentKeyword());

            // 设置元数据
            Map<String, Object> metadata = new HashMap<>();
            metadata.put("document_id", chunk.getDocumentId());
            metadata.put("dataset_id", chunk.getDatasetIdAsString());

            if (chunk.getImportantKeywords() != null && !chunk.getImportantKeywords().isEmpty()) {
                metadata.put("keywords", chunk.getImportantKeywords());
            }

            if (chunk.getHighlight() != null) {
                metadata.put("highlight", chunk.getHighlight());
            }

            if (chunk.getPositions() != null && !chunk.getPositions().isEmpty()) {
                metadata.put("positions", chunk.getPositions());
            }

            record.setMetadata(metadata);

            records.add(record);
        }

        difyResp.setRecords(records);
        return difyResp;
    }
} 