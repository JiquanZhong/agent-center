package com.diit.ds.service;

import com.diit.ds.domain.req.DifyKnowledgeReq;
import com.diit.ds.domain.resp.DifyKnowledgeResp;

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
} 