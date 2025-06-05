package com.diit.ds.rag.service;

import com.diit.ds.rag.domain.req.RAGFlowFileDeleteReq;
import com.diit.ds.rag.domain.req.RAGFlowFileListReq;
import com.diit.ds.rag.domain.req.RAGFlowFileParseReq;
import com.diit.ds.rag.domain.resp.RAGFlowFileDeleteResp;
import com.diit.ds.rag.domain.resp.RAGFlowFileListResp;
import com.diit.ds.rag.domain.resp.RAGFlowFileParseResp;
import com.diit.ds.rag.domain.resp.RAGFlowFileUploadResp;
import org.springframework.web.multipart.MultipartFile;

import java.io.OutputStream;

public interface RAGFlowFileAPIService {
    
    /**
     * 上传文件到指定数据集
     * @param datasetId 数据集ID
     * @param files 文件列表
     * @return 上传结果
     */
    RAGFlowFileUploadResp uploadFiles(String datasetId, MultipartFile[] files);
    
    /**
     * 下载文件
     * @param datasetId 数据集ID
     * @param documentId 文档ID
     * @param outputStream 输出流
     * @return 下载结果
     */
    boolean downloadFile(String datasetId, String documentId, OutputStream outputStream);
    
    /**
     * 查看文件列表
     * @param datasetId 数据集ID
     * @param req 查询条件
     * @return 文件列表
     */
    RAGFlowFileListResp listFiles(String datasetId, RAGFlowFileListReq req);
    
    /**
     * 删除文件
     * @param datasetId 数据集ID
     * @param req 删除文件请求
     * @return 删除结果
     */
    RAGFlowFileDeleteResp deleteFiles(String datasetId, RAGFlowFileDeleteReq req);
    
    /**
     * 开始解析文件任务
     * @param datasetId 数据集ID
     * @param req 解析文件请求
     * @return 解析结果
     */
    RAGFlowFileParseResp startParseTask(String datasetId, RAGFlowFileParseReq req);
    
    /**
     * 停止解析文件任务
     * @param datasetId 数据集ID
     * @param req 停止解析文件请求
     * @return 停止结果
     */
    RAGFlowFileParseResp stopParseTask(String datasetId, RAGFlowFileParseReq req);
}
