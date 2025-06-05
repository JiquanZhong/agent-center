package com.diit.ds.rag.service.impl;

import com.diit.ds.rag.domain.req.DifyKnowledgeHttpReq;
import com.diit.ds.rag.domain.req.DifyKnowledgeReq;
import com.diit.ds.rag.domain.req.DiosRetrieveReq;
import com.diit.ds.rag.domain.resp.DifyKnowledgeHttpResp;
import com.diit.ds.rag.domain.resp.DifyKnowledgeResp;
import com.diit.ds.rag.domain.resp.DiosRetrieveResp;
import com.diit.ds.domain.pojo.entity.WorkflowChunk;
import com.diit.ds.domain.pojo.entity.WorkflowDocAgg;
import com.diit.ds.domain.pojo.entity.WorkflowRun;
import com.diit.ds.rag.service.DifyKnowledgeService;
import com.diit.ds.rag.service.DiosAPIService;
import com.diit.ds.rag.service.WorkflowRunService;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Service("dios2DifyKnowledgeServiceImpl")
@RequiredArgsConstructor
public class Dios2DifyKnowledgeServiceImpl implements DifyKnowledgeService {

    private final DiosAPIService diosAPIService;
    private final WorkflowRunService workflowRunService;
    private final WorkflowDocAggServiceImpl workflowDocAggService;
    private final WorkflowChunkServiceImpl workflowChunkService;
    private final ObjectMapper objectMapper;


    @Override
    public DifyKnowledgeResp retrieveKnowledge(DifyKnowledgeReq req) {
        try {
            // 将Dify请求转换为DIOS请求
            DiosRetrieveReq diosReq = convertToDiosRequest(req);

            // 调用DIOS API
            DiosRetrieveResp diosResp = diosAPIService.retrieve(diosReq);

            // 将DIOS响应转换为Dify响应
            return convertToDifyResponse(diosResp);
        } catch (Exception e) {
            log.error("调用DIOS API失败", e);
            // 返回空结果
            return new DifyKnowledgeResp();
        }
    }

    @Override
    public DifyKnowledgeHttpResp retrieveKnowledgeHttp(DifyKnowledgeHttpReq req) {
        try {
            log.info("执行DIOS检索操作，转换为Dify HTTP响应，请求参数：{}", req);
            
            // 将Dify请求转换为DIOS请求
            DiosRetrieveReq diosReq = convertToDiosRequest(req);
            
            // 调用DIOS API
            DiosRetrieveResp diosResp = diosAPIService.retrieve(diosReq);
            
            // 将DIOS响应转换为Dify HTTP响应
            DifyKnowledgeHttpResp difyResp = convertToDifyHttpResponse(diosResp);
            
            // 根据work_flow_run_id，把此次查询的结果存储到数据库中，实现高亮效果
            persistDiosResponse(diosResp, req.getWorkFlowRunId(), req.getQuery());
            
            log.info("DIOS检索操作完成，返回结果数量：{}", difyResp.getRecords().size());
            return difyResp;
        } catch (Exception e) {
            log.error("调用DIOS API失败", e);
            // 返回空结果
            return new DifyKnowledgeHttpResp();
        }
    }
    
    /**
     * 将Dify请求转换为DIOS请求
     */
    private DiosRetrieveReq convertToDiosRequest(DifyKnowledgeReq req) {
        DiosRetrieveReq diosReq = new DiosRetrieveReq();
        
        // 设置查询文本
        diosReq.setQuery(req.getQuery());
        
        // 设置检索参数
        if (req.getRetrievalSetting() != null) {
            if (req.getRetrievalSetting().getTopK() != null) {
                diosReq.setTopK(req.getRetrievalSetting().getTopK().toString());
            } else {
                diosReq.setTopK("5"); // 默认值
            }
            
            if (req.getRetrievalSetting().getScoreThreshold() != null) {
                diosReq.setScoreThreshold(req.getRetrievalSetting().getScoreThreshold());
            } else {
                diosReq.setScoreThreshold(0.2); // 默认值
            }
        } else {
            diosReq.setTopK("5");
            diosReq.setScoreThreshold(0.2);
        }
        
        return diosReq;
    }
    
    /**
     * 将Dify HTTP请求转换为DIOS请求
     */
    private DiosRetrieveReq convertToDiosRequest(DifyKnowledgeHttpReq req) {
        DiosRetrieveReq diosReq = new DiosRetrieveReq();
        
        // 设置查询文本
        diosReq.setQuery(req.getQuery());
        
        // 设置检索参数
        if (req.getRetrievalSetting() != null) {
            if (req.getRetrievalSetting().getTopK() != null) {
                diosReq.setTopK(req.getRetrievalSetting().getTopK().toString());
            } else {
                diosReq.setTopK("5"); // 默认值
            }
            
            if (req.getRetrievalSetting().getScoreThreshold() != null) {
                diosReq.setScoreThreshold(req.getRetrievalSetting().getScoreThreshold());
            } else {
                diosReq.setScoreThreshold(0.2); // 默认值
            }
        } else {
            diosReq.setTopK("5");
            diosReq.setScoreThreshold(0.2);
        }
        
        return diosReq;
    }
    
    /**
     * 将DIOS响应转换为Dify响应
     */
    private DifyKnowledgeResp convertToDifyResponse(DiosRetrieveResp diosResp) {
        DifyKnowledgeResp difyResp = new DifyKnowledgeResp();
        
        if (diosResp == null || diosResp.getRetrieveResults() == null || diosResp.getRetrieveResults().isEmpty()) {
            difyResp.setRecords(Collections.emptyList());
            return difyResp;
        }
        
        List<DifyKnowledgeResp.Record> records = new ArrayList<>();
        
        for (DiosRetrieveResp.RetrieveResult result : diosResp.getRetrieveResults()) {
            DifyKnowledgeResp.Record record = new DifyKnowledgeResp.Record();
            
            // 设置文档内容
            record.setContent(result.getText());
            
            // 设置相关性分数
            record.setScore(result.getScore());
            
            // 设置文档标题
            record.setTitle(result.getFilename());
            
            records.add(record);
        }
        
        difyResp.setRecords(records);
        return difyResp;
    }
    
    /**
     * 将DIOS响应转换为Dify HTTP响应
     */
    private DifyKnowledgeHttpResp convertToDifyHttpResponse(DiosRetrieveResp diosResp) {
        DifyKnowledgeHttpResp difyResp = new DifyKnowledgeHttpResp();
        
        if (diosResp == null || diosResp.getRetrieveResults() == null || diosResp.getRetrieveResults().isEmpty()) {
            difyResp.setRecords(Collections.emptyList());
            return difyResp;
        }
        
        List<DifyKnowledgeHttpResp.SimpleRecord> records = new ArrayList<>();
        Integer index = 1;
        
        for (DiosRetrieveResp.RetrieveResult result : diosResp.getRetrieveResults()) {
            DifyKnowledgeHttpResp.SimpleRecord record = new DifyKnowledgeHttpResp.SimpleRecord();
            
            // 设置文档内容
            record.setContent(result.getText());
            
            // 设置相关性分数
            record.setScore(result.getScore());
            
            // 设置文档标题
            record.setTitle(result.getFilename());
            
            // 设置序列号
            record.setIndex(index++);
            
            records.add(record);
        }
        
        difyResp.setRecords(records);
        return difyResp;
    }
    
    /**
     * 持久化DIOS响应数据
     * 
     * @param diosResp DIOS响应结果
     * @param workFlowRunId 工作流运行ID
     * @param queryText 查询文本
     */
    private void persistDiosResponse(DiosRetrieveResp diosResp, String workFlowRunId, String queryText) {
        if (diosResp == null || workFlowRunId == null || workFlowRunId.trim().isEmpty()) {
            log.warn("无法持久化DIOS响应：响应为空或工作流运行ID为空");
            return;
        }
        
        try {
            // 1. 保存工作流运行记录
            WorkflowRun workflowRun = new WorkflowRun();
            workflowRun.setWorkFlowRunId(workFlowRunId);
            workflowRun.setCode(0); // 假设成功
            workflowRun.setMessage("success");
            workflowRun.setQueryText(queryText);
            workflowRun.setTotal(diosResp.getRetrieveResults().size());
            
            workflowRunService.save(workflowRun);
            
            // 2. 保存文档片段记录
            if (diosResp.getRetrieveResults() != null && !diosResp.getRetrieveResults().isEmpty()) {
                List<WorkflowChunk> chunkList = new ArrayList<>();
                
                Integer index = 1;
                // 遍历文档片段
                for (DiosRetrieveResp.RetrieveResult result : diosResp.getRetrieveResults()) {
                    WorkflowChunk workflowChunk = new WorkflowChunk();
                    workflowChunk.setWorkFlowRunId(workFlowRunId);
                    workflowChunk.setChunkId(String.valueOf(index)); // 使用index作为chunkId
                    workflowChunk.setDocumentId(String.valueOf(result.getResourceId()));
                    workflowChunk.setContent(result.getText());
                    workflowChunk.setDocumentKeyword(result.getFilename()); // 使用filename作为文档关键词
                    workflowChunk.setSimilarity(result.getScore());
                    workflowChunk.setIndex(index++);
                    
                    chunkList.add(workflowChunk);
                }
                
                if (!chunkList.isEmpty()) {
                    workflowChunkService.saveBatch(chunkList);
                }
            }
            
            // 3. 按照文档聚合统计信息
            if (diosResp.getRetrieveResults() != null && !diosResp.getRetrieveResults().isEmpty()) {
                // 统计每个文档的出现次数
                Map<Integer, Integer> docCountMap = new HashMap<>();
                Map<Integer, String> docNameMap = new HashMap<>();
                
                // 遍历所有结果，记录每个文档的出现次数和名称
                for (DiosRetrieveResp.RetrieveResult result : diosResp.getRetrieveResults()) {
                    Integer resourceId = result.getResourceId();
                    docCountMap.put(resourceId, docCountMap.getOrDefault(resourceId, 0) + 1);
                    docNameMap.put(resourceId, result.getFilename());
                }
                
                // 创建文档聚合记录
                List<WorkflowDocAgg> docAggList = new ArrayList<>();
                
                for (Map.Entry<Integer, Integer> entry : docCountMap.entrySet()) {
                    Integer resourceId = entry.getKey();
                    Integer count = entry.getValue();
                    String docName = docNameMap.get(resourceId);
                    
                    WorkflowDocAgg workflowDocAgg = new WorkflowDocAgg();
                    workflowDocAgg.setWorkFlowRunId(workFlowRunId);
                    workflowDocAgg.setDocId(String.valueOf(resourceId));
                    workflowDocAgg.setDocName(docName);
                    workflowDocAgg.setCount(count);
                    
                    docAggList.add(workflowDocAgg);
                }
                
                if (!docAggList.isEmpty()) {
                    workflowDocAggService.saveBatch(docAggList);
                }
            }
            
            log.info("成功持久化DIOS响应数据，workFlowRunId: {}", workFlowRunId);
        } catch (Exception e) {
            log.error("持久化DIOS响应数据失败", e);
        }
    }
}
