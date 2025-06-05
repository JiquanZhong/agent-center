package com.diit.ds.rag.service;

import com.diit.ds.rag.domain.req.DiosRetrieveReq;
import com.diit.ds.rag.domain.resp.DiosRetrieveResp;

/**
 * DIOS API服务接口
 */
public interface DiosAPIService {
    
    /**
     * 执行检索操作
     * 
     * @param retrieveReq 检索请求参数
     * @return 检索结果
     */
    DiosRetrieveResp retrieve(DiosRetrieveReq retrieveReq);
}
