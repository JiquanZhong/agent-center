package com.diit.ds.service;

import com.diit.ds.domain.req.RAGFlowDatasetCreateReq;
import com.diit.ds.domain.req.RAGFlowDatasetDeleteReq;
import com.diit.ds.domain.req.RAGFlowDatasetListReq;
import com.diit.ds.domain.req.RAGFlowDatasetUpdateReq;
import com.diit.ds.domain.req.RAGFlowKnowledgeReq;
import com.diit.ds.domain.resp.RAGFlowDatasetCreateResp;
import com.diit.ds.domain.resp.RAGFlowDatasetDeleteResp;
import com.diit.ds.domain.resp.RAGFlowDatasetListResp;
import com.diit.ds.domain.resp.RAGFlowDatasetUpdateResp;
import com.diit.ds.domain.resp.RAGFlowKnowledgeResp;

public interface RAGFlowAPIService {
    /**
     * 知识检索
     * @param req 检索请求
     * @return 检索结果
     */
    RAGFlowKnowledgeResp retrieval(RAGFlowKnowledgeReq req);

    /**
     * 创建数据集
     * @param req 创建数据集请求
     * @return 创建数据集响应
     */
    RAGFlowDatasetCreateResp createDataset(RAGFlowDatasetCreateReq req);
    
    /**
     * 删除数据集
     * @param req 删除数据集请求
     * @return 删除数据集响应
     */
    RAGFlowDatasetDeleteResp deleteDatasets(RAGFlowDatasetDeleteReq req);
    
    /**
     * 更新数据集
     * @param datasetId 数据集ID
     * @param req 更新数据集请求
     * @return 更新数据集响应
     */
    RAGFlowDatasetUpdateResp updateDataset(String datasetId, RAGFlowDatasetUpdateReq req);
    
    /**
     * 获取数据集列表
     * @param req 列表数据集请求
     * @return 列表数据集响应
     */
    RAGFlowDatasetListResp listDatasets(RAGFlowDatasetListReq req);
}
