package com.diit.ds.rag.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.diit.ds.rag.domain.req.AgentKnowledgeHttpReq;
import com.diit.ds.rag.domain.resp.DifyKnowledgeHttpResp;
import com.diit.ds.domain.pojo.entity.AgentKdb;

/**
* @author test
* @description 针对表【agent_kdb(智能体 知识库id关联表)】的数据库操作Service
* @createDate 2025-05-28 14:39:11
*/
public interface AgentKdbService extends IService<AgentKdb> {
    /**
     * 通过http请求，从知识库中检索相关内容
     *
     * @param req 检索请求
     * @return 检索结果
     */
    DifyKnowledgeHttpResp retrieveKnowledgeHttp(AgentKnowledgeHttpReq req);
}
