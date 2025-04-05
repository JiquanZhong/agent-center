package com.diit.ds.service.impl;

import com.diit.ds.domain.entity.KnowledgeTreeNode;
import com.diit.ds.domain.entity.WorkflowRun;
import com.diit.ds.domain.entity.WorkflowChunk;
import com.diit.ds.domain.entity.WorkflowDocAgg;
import com.diit.ds.domain.req.DifyKnowledgeHttpReq;
import com.diit.ds.domain.req.DifyKnowledgeReq;
import com.diit.ds.domain.req.RAGFlowKnowledgeReq;
import com.diit.ds.domain.resp.DifyKnowledgeHttpResp;
import com.diit.ds.domain.resp.DifyKnowledgeResp;
import com.diit.ds.domain.resp.RAGFlowKnowledgeResp;
import com.diit.ds.service.DifyKnowledgeService;
import com.diit.ds.service.KnowledgeTreeNodeService;
import com.diit.ds.service.RAGFlowDBAPIService;
import com.diit.ds.service.WorkflowRunService;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
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
    private final WorkflowRunService workflowRunService;
    private final WorkflowDocAggServiceImpl workflowDocAggService;
    private final WorkflowChunkServiceImpl workflowChunkService;
    private final ObjectMapper objectMapper;

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

    @Override
    public DifyKnowledgeHttpResp retrieveKnowledgeHttp(DifyKnowledgeHttpReq req) {
        // 将Dify请求转换为RAGFlow请求
        RAGFlowKnowledgeReq ragFlowReq = convertToRAGFlowRequest(req);

        // 调用RAGFlow API
        RAGFlowKnowledgeResp ragFlowResp = ragFlowDBAPIService.retrieval(ragFlowReq);

        // 将RAGFlow响应转换为Dify响应
        DifyKnowledgeHttpResp difyResp = convertToDifyHttpResponse(ragFlowResp);

        // 根据work_flow_run_id，把此次查询的结果存储到数据库中，实现高亮效果
        persistRagFlowResponse(ragFlowResp, req.getWorkFlowRunId(), req.getQuery());


        return difyResp;
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
     * 将Dify请求转换为RAGFlow请求
     */
    private RAGFlowKnowledgeReq convertToRAGFlowRequest(DifyKnowledgeHttpReq req) {
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

    /**
     * 将RAGFlow响应转换为Dify响应
     */
    private DifyKnowledgeHttpResp convertToDifyHttpResponse(RAGFlowKnowledgeResp ragFlowResp) {
        DifyKnowledgeHttpResp difyResp = new DifyKnowledgeHttpResp();

        if (ragFlowResp == null || ragFlowResp.getData() == null || ragFlowResp.getData().getChunks() == null) {
            difyResp.setRecords(Collections.emptyList());
            return difyResp;
        }

        List<DifyKnowledgeHttpResp.SimpleRecord> records = getSimpleRecords(ragFlowResp);

        difyResp.setRecords(records);
        return difyResp;
    }

    private static List<DifyKnowledgeHttpResp.SimpleRecord> getSimpleRecords(RAGFlowKnowledgeResp ragFlowResp) {
        List<DifyKnowledgeHttpResp.SimpleRecord> records = new ArrayList<>();
        Integer index = 1;

        for (RAGFlowKnowledgeResp.RespData.Chunk chunk : ragFlowResp.getData().getChunks()) {
            DifyKnowledgeHttpResp.SimpleRecord record = new DifyKnowledgeHttpResp.SimpleRecord();

            // 设置文档内容
            record.setContent(chunk.getContent());

            // 设置相关性分数
            record.setScore(chunk.getSimilarity());

            // 设置文档标题
            record.setTitle(chunk.getDocumentKeyword());

            // 设置序列号
            record.setIndex(index++);

            records.add(record);
        }
        return records;
    }

    /**
     * 持久化RAGFlow响应数据
     * 
     * @param ragFlowResp RAGFlow响应结果
     * @param workFlowRunId 工作流运行ID
     * @param queryText 查询文本
     */
    private void persistRagFlowResponse(RAGFlowKnowledgeResp ragFlowResp, String workFlowRunId, String queryText) {
        if (ragFlowResp == null || workFlowRunId == null || workFlowRunId.trim().isEmpty()) {
            log.warn("无法持久化RAGFlow响应：响应为空或工作流运行ID为空");
            return;
        }

        try {
            // 1. 保存工作流运行记录
            WorkflowRun workflowRun = new WorkflowRun();
            workflowRun.setWorkFlowRunId(workFlowRunId);
            workflowRun.setCode(ragFlowResp.getCode());
            workflowRun.setMessage(ragFlowResp.getMessage());
            workflowRun.setQueryText(queryText);
            
            if (ragFlowResp.getData() != null) {
                workflowRun.setTotal(ragFlowResp.getData().getTotal());
            }
            
            workflowRunService.save(workflowRun);
            
            // 2. 保存文档片段记录
            if (ragFlowResp.getData() != null && ragFlowResp.getData().getChunks() != null) {
                List<WorkflowChunk> chunkList = new ArrayList<>();

                Integer index = 1;
                // 遍历文档片段
                for (RAGFlowKnowledgeResp.RespData.Chunk chunk : ragFlowResp.getData().getChunks()) {
                    WorkflowChunk workflowChunk = new WorkflowChunk();
                    workflowChunk.setWorkFlowRunId(workFlowRunId);
                    workflowChunk.setChunkId(chunk.getId());
                    workflowChunk.setContent(chunk.getContent());
                    workflowChunk.setContentLtks(chunk.getContentLtks());
                    workflowChunk.setDatasetId(chunk.getDatasetIdAsString());
                    workflowChunk.setDocumentId(chunk.getDocumentId());
                    workflowChunk.setDocumentKeyword(chunk.getDocumentKeyword());
                    workflowChunk.setHighlight(chunk.getHighlight());
                    workflowChunk.setImageId(chunk.getImageId());
                    workflowChunk.setKbId(chunk.getKbId());
                    workflowChunk.setSimilarity(chunk.getSimilarity());
                    workflowChunk.setTermSimilarity(chunk.getTermSimilarity());
                    workflowChunk.setVectorSimilarity(chunk.getVectorSimilarity());
                    workflowChunk.setIndex(index++);
                    
                    // 将List类型转换为JSON字符串
                    if (chunk.getImportantKeywords() != null) {
                        try {
                            String keywordsJson = objectMapper.writeValueAsString(chunk.getImportantKeywords());
                            workflowChunk.setImportantKeywords(keywordsJson);
                        } catch (JsonProcessingException e) {
                            log.error("转换importantKeywords为JSON失败", e);
                            workflowChunk.setImportantKeywords("[]");
                        }
                    } else {
                        workflowChunk.setImportantKeywords("[]");
                    }
                    
                    // 将二维数组转换为JSON字符串存储
                    if (chunk.getPositions() != null && !chunk.getPositions().isEmpty()) {
                        try {
                            String positionsJson = objectMapper.writeValueAsString(chunk.getPositions());
                            workflowChunk.setPositions(positionsJson);
                        } catch (JsonProcessingException e) {
                            log.error("转换positions为JSON失败", e);
                            workflowChunk.setPositions("[]");
                        }
                    } else {
                        workflowChunk.setPositions("[]");
                    }
                    
                    chunkList.add(workflowChunk);
                }
                
                if (!chunkList.isEmpty()) {
                    workflowChunkService.saveBatch(chunkList);
                }
            }
            
            // 3. 保存文档聚合记录
            if (ragFlowResp.getData() != null && ragFlowResp.getData().getDocAggs() != null) {
                List<WorkflowDocAgg> docAggList = new ArrayList<>();
                
                for (RAGFlowKnowledgeResp.RespData.DocAgg docAgg : ragFlowResp.getData().getDocAggs()) {
                    WorkflowDocAgg workflowDocAgg = new WorkflowDocAgg();
                    workflowDocAgg.setWorkFlowRunId(workFlowRunId);
                    workflowDocAgg.setDocId(docAgg.getDocId());
                    workflowDocAgg.setDocName(docAgg.getDocName());
                    workflowDocAgg.setCount(docAgg.getCount());
                    
                    docAggList.add(workflowDocAgg);
                }
                
                if (!docAggList.isEmpty()) {
                    workflowDocAggService.saveBatch(docAggList);
                }
            }
            
            log.info("成功持久化RAGFlow响应数据，workFlowRunId: {}", workFlowRunId);
        } catch (Exception e) {
            log.error("持久化RAGFlow响应数据失败", e);
        }
    }
} 