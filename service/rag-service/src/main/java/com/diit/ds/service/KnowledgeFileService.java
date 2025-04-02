package com.diit.ds.service;

import com.diit.ds.domain.req.*;
import com.diit.ds.domain.resp.*;
import com.diit.ds.exception.FileNotFoundException;
import org.springframework.web.multipart.MultipartFile;

import java.io.OutputStream;

/**
* @author test
* @createDate 2025-03-31 13:11:23
*/
public interface KnowledgeFileService{

    /**
     * 上传文件到知识中心节点
     * @param treeNodeId 数据集ID
     * @param files 文件列表
     * @return 上传结果
     */
    RAGFlowFileUploadResp uploadFiles(String treeNodeId, MultipartFile[] files);

    /**
     * 下载文件
     * @param documentId 文档ID
     * @param outputStream 输出流
     * @return 下载结果
     */
    String downloadFile(String documentId, OutputStream outputStream) throws FileNotFoundException;

    /**
     * 查看文件列表
     * @param treeNodeId 数据集ID
     * @param req 查询条件
     * @return 文件列表
     */
    RAGFlowFileListResp listFiles(String treeNodeId, RAGFlowFileListReq req);

    /**
     * 删除文件
     * @param req 删除文件请求
     * @return 删除结果
     */
    RAGFlowFileDeleteResp deleteFiles(RAGFlowFileDeleteReq req);

    /**
     * 开始解析文件任务
     * @param req 解析文件请求
     * @return 解析结果
     */
    RAGFlowFileParseResp startParseTask(RAGFlowFileParseReq req);

    /**
     * 停止解析文件任务
     * @param req 停止解析文件请求
     * @return 停止结果
     */
    RAGFlowFileParseResp stopParseTask(RAGFlowFileParseReq req);

    /**
     * 查看文档的切片列表
     * @param documentId 文档ID
     * @param req 查询条件
     * @return 切片列表响应
     */
    RagFlowChunkListResp listChunks(String documentId, RagFlowChunkListReq req);

    /**
     * 更新文档切片内容
     * @param chunkId 切片ID
     * @param req 更新请求
     * @return 更新响应
     */
    RagFlowChunkUpdateResp updateChunk(String chunkId, RagFlowChunkUpdateReq req);
}
