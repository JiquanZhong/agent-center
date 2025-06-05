package com.diit.ds.rag.service;

import com.diit.ds.rag.domain.req.RagFlowChunkListReq;
import com.diit.ds.rag.domain.req.RagFlowChunkUpdateReq;
import com.diit.ds.rag.domain.resp.RagFlowChunkListResp;
import com.diit.ds.rag.domain.resp.RagFlowChunkUpdateResp;

/**
 * RAGFlow文件切片API服务接口
 */
public interface RAGFlowFileChunkAPIService {
    
    /**
     * 查看文档的切片列表
     * @param datasetId 数据集ID
     * @param documentId 文档ID
     * @param req 查询条件
     * @return 切片列表响应
     */
    RagFlowChunkListResp listChunks(String datasetId, String documentId, RagFlowChunkListReq req);
    
    /**
     * 更新文档切片内容
     * @param datasetId 数据集ID
     * @param documentId 文档ID
     * @param chunkId 切片ID
     * @param req 更新请求
     * @return 更新响应
     */
    RagFlowChunkUpdateResp updateChunk(String datasetId, String documentId, String chunkId, RagFlowChunkUpdateReq req);
}
