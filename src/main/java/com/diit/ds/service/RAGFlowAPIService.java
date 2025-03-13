package com.diit.ds.service;

import com.diit.ds.domain.req.RAGFlowKnowledgeReq;
import com.diit.ds.domain.resp.RAGFlowKnowledgeResp;

public interface RAGFlowAPIService {
    RAGFlowKnowledgeResp retrieval(RAGFlowKnowledgeReq req);
}
