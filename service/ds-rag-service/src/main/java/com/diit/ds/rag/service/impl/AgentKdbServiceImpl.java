package com.diit.ds.rag.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.diit.ds.domain.mapper.AgentKdbMapper;
import com.diit.ds.rag.domain.req.AgentKnowledgeHttpReq;
import com.diit.ds.rag.domain.req.RAGFlowKnowledgeReq;
import com.diit.ds.rag.domain.resp.DifyKnowledgeHttpResp;
import com.diit.ds.rag.domain.resp.RAGFlowKnowledgeResp;
import com.diit.ds.domain.pojo.entity.AgentKdb;
import com.diit.ds.domain.pojo.entity.WorkflowChunk;
import com.diit.ds.domain.pojo.entity.WorkflowDocAgg;
import com.diit.ds.domain.pojo.entity.WorkflowRun;
import com.diit.ds.rag.service.AgentKdbService;
import com.diit.ds.rag.service.RAGFlowDBAPIService;
import com.diit.ds.rag.service.WorkflowRunService;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
* @author test
* @description 针对表【agent_kdb(智能体 知识库id关联表)】的数据库操作Service实现
* @createDate 2025-05-28 14:39:11
*/
@Service
@Slf4j
@RequiredArgsConstructor
public class AgentKdbServiceImpl extends ServiceImpl<AgentKdbMapper, AgentKdb>
    implements AgentKdbService{

    private final RAGFlowDBAPIService ragFlowDBAPIService;
    private final WorkflowRunService workflowRunService;
    private final WorkflowDocAggServiceImpl workflowDocAggService;
    private final WorkflowChunkServiceImpl workflowChunkService;
    private final ObjectMapper objectMapper;

    @Override
    public DifyKnowledgeHttpResp retrieveKnowledgeHttp(AgentKnowledgeHttpReq req) {
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
    private RAGFlowKnowledgeReq convertToRAGFlowRequest(AgentKnowledgeHttpReq req) {
        RAGFlowKnowledgeReq ragFlowReq = new RAGFlowKnowledgeReq();

        // 设置查询
        ragFlowReq.setQuestion(req.getQuery());

        // 设置知识库ID
        String appId = req.getAppId();
        List<AgentKdb> list = lambdaQuery().eq(AgentKdb::getAppId, appId).list();
        if (list == null || list.isEmpty()) {
            log.error("知识库不能为空，appId：{}", appId);
            throw new RuntimeException("知识库不能为空");
        }

        List<String> kdbIds = list
                .stream()
                .map(AgentKdb::getKdbId)
                .toList();

        ragFlowReq.setDatasetIds(kdbIds);

        // 设置检索参数
        if (req.getRetrievalSetting() != null) {
            if (req.getRetrievalSetting().getTopK() != null) {
                ragFlowReq.setTopK(req.getRetrievalSetting().getTopK());
            }

            if (req.getRetrievalSetting().getScoreThreshold() != null) {
                ragFlowReq.setSimilarityThreshold(req.getRetrievalSetting().getScoreThreshold());
            }

            if (req.getRetrievalSetting().getVectorSimilarityWeight() != null) {
                ragFlowReq.setVectorSimilarityWeight(req.getRetrievalSetting().getVectorSimilarityWeight());
            }
        }

        ragFlowReq.setPage(1);
        ragFlowReq.setPageSize(req.getRetrievalSetting().getTopK());


        return ragFlowReq;
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
}




