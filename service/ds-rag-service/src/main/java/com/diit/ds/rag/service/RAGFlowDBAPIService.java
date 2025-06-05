package com.diit.ds.rag.service;

import com.diit.ds.rag.domain.req.RAGFlowDatasetCreateReq;
import com.diit.ds.rag.domain.req.RAGFlowDatasetDeleteReq;
import com.diit.ds.rag.domain.req.RAGFlowDatasetListReq;
import com.diit.ds.rag.domain.req.RAGFlowDatasetUpdateReq;
import com.diit.ds.rag.domain.req.RAGFlowKnowledgeReq;
import com.diit.ds.rag.domain.resp.RAGFlowDatasetCreateResp;
import com.diit.ds.rag.domain.resp.RAGFlowDatasetDeleteResp;
import com.diit.ds.rag.domain.resp.RAGFlowDatasetListResp;
import com.diit.ds.rag.domain.resp.RAGFlowDatasetUpdateResp;
import com.diit.ds.rag.domain.resp.RAGFlowKnowledgeResp;

/**
 * RAGFlow API服务接口
 */
public interface RAGFlowDBAPIService {
    /**
     * 知识检索
     * @param req 检索请求
     * @return 检索结果
     */
    RAGFlowKnowledgeResp retrieval(RAGFlowKnowledgeReq req);

    /**
     * 创建数据集
     *
     * @param req 创建数据集请求
     * @return 创建的数据集信息
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
    
    /**
     * 创建通用类型数据集
     * 
     * @param name 数据集名称
     * @param description 数据集描述
     * @param permission 权限设置，默认为"me"
     * @return 创建的数据集信息
     */
    RAGFlowDatasetCreateResp createGeneralDataset(String name, String description, String permission, String delimiter, Integer chunkTokenNum, Integer autoKeywords, Integer autoQuestions);
    
    /**
     * 创建法律类型数据集
     * 
     * @param name 数据集名称
     * @param description 数据集描述
     * @param permission 权限设置，默认为"me"
     * @return 创建的数据集信息
     */
    RAGFlowDatasetCreateResp createLawsDataset(String name, String description, String permission, String delimiter, Integer chunkTokenNum, Integer autoKeywords, Integer autoQuestions);
    
    /**
     * 创建论文类型数据集
     * 
     * @param name 数据集名称
     * @param description 数据集描述
     * @param permission 权限设置，默认为"me"
     * @return 创建的数据集信息
     */
    RAGFlowDatasetCreateResp createPaperDataset(String name, String description, String permission, String delimiter, Integer chunkTokenNum, Integer autoKeywords, Integer autoQuestions);
    
    /**
     * 创建书籍类型数据集
     * 
     * @param name 数据集名称
     * @param description 数据集描述
     * @param permission 权限设置，默认为"me"
     * @return 创建的数据集信息
     */
    RAGFlowDatasetCreateResp createBookDataset(String name, String description, String permission, String delimiter, Integer chunkTokenNum, Integer autoKeywords, Integer autoQuestions);
    
    /**
     * 创建问答对类型数据集
     * 
     * @param name 数据集名称
     * @param description 数据集描述
     * @param permission 权限设置，默认为"me"
     * @return 创建的数据集信息
     */
    RAGFlowDatasetCreateResp createQADataset(String name, String description, String permission, String delimiter, Integer chunkTokenNum, Integer autoKeywords, Integer autoQuestions);
}
