package com.diit.ds.rag.service;

import com.diit.ds.rag.domain.req.DifyKnowledgeHttpReq;
import com.diit.ds.rag.domain.req.DifyKnowledgeReq;
import com.diit.ds.rag.domain.resp.DifyKnowledgeHttpResp;
import com.diit.ds.rag.domain.resp.DifyKnowledgeResp;

/**
 * Dify外部知识库API服务接口
 */
public interface DifyKnowledgeService {
    
    /**
     * 从知识库中检索相关内容
     * 
     * @param req 检索请求
     * @return 检索结果
     */
    DifyKnowledgeResp retrieveKnowledge(DifyKnowledgeReq req);

    /**
     * 通过http请求，从知识库中检索相关内容
     *
     * @param req 检索请求
     * @return 检索结果
     */
    DifyKnowledgeHttpResp retrieveKnowledgeHttp(DifyKnowledgeHttpReq req);
} 