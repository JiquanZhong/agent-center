package com.diit.ds.service.impl;

import com.baomidou.dynamic.datasource.annotation.DS;
import com.diit.ds.domain.entity.Document;
import com.diit.ds.domain.entity.KnowledgeTreeNode;
import com.diit.ds.domain.req.*;
import com.diit.ds.domain.resp.*;
import com.diit.ds.exception.FileNotFoundException;
import com.diit.ds.service.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.OutputStream;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class KnowledgeFileServiceImpl implements KnowledgeFileService {

    private final KnowledgeTreeNodeService knowledgeTreeNodeService;
    private final DocumentService documentService;
    private final RAGFlowFileAPIService ragFlowFileAPIService;
    private final RagFlowFileChunkAPIService ragFlowFileChunkAPIService;

    @Override
    public RAGFlowFileUploadResp uploadFiles(String treeNodeId, MultipartFile[] files) {
        KnowledgeTreeNode treeNode = knowledgeTreeNodeService.lambdaQuery()
                .eq(KnowledgeTreeNode::getId, treeNodeId)
                .one();
        
        if (treeNode == null) {
            log.error("未找到指定的知识库节点: {}", treeNodeId);
            RAGFlowFileUploadResp errorResp = new RAGFlowFileUploadResp();
            errorResp.setCode(404);
            errorResp.setMessage("未找到指定的知识库节点");
            return errorResp;
        }
        
        String datasetId = treeNode.getKdbId();
        return ragFlowFileAPIService.uploadFiles(datasetId, files);
    }

    @Override
    public String downloadFile(String documentId, OutputStream outputStream) throws FileNotFoundException {
        Document document = documentService.lambdaQuery()
                .eq(Document::getId, documentId)
                .one();
        
        if (document == null) {
            throw new FileNotFoundException("文件不存在");
        }

        String datasetId = document.getKbId();

        if (datasetId == null) {
            throw new FileNotFoundException("文件不存在");
        }

        boolean success = ragFlowFileAPIService.downloadFile(datasetId, documentId, outputStream);

        if (success) {
            return document.getName();
        }

        return null;
    }

    @Override
    public RAGFlowFileListResp listFiles(String treeNodeId, RAGFlowFileListReq req) {
        KnowledgeTreeNode treeNode = knowledgeTreeNodeService.lambdaQuery()
                .eq(KnowledgeTreeNode::getId, treeNodeId)
                .one();
        
        if (treeNode == null) {
            log.error("未找到指定的知识库节点: {}", treeNodeId);
            RAGFlowFileListResp errorResp = new RAGFlowFileListResp();
            errorResp.setCode(404);
            errorResp.setMessage("未找到指定的知识库节点");
            return errorResp;
        }

        String datasetId = treeNode.getKdbId();
        return ragFlowFileAPIService.listFiles(datasetId, req);
    }

    @Override
    public RAGFlowFileDeleteResp deleteFiles(RAGFlowFileDeleteReq req) {
        RAGFlowFileDeleteResp resp = null;
        for (String documentId : req.getIds()) {
            Document document = documentService.lambdaQuery()
                    .eq(Document::getId, documentId)
                    .one();
            
            if (document == null) {
                log.error("未找到指定的文档: {}", documentId);
                continue;
            }

            String datasetId = document.getId();

            if (datasetId != null) {
                resp = ragFlowFileAPIService.deleteFiles(datasetId, req);
            }
        }
        return resp;
    }

    @Override
    public RAGFlowFileParseResp startParseTask(RAGFlowFileParseReq req) {
        RAGFlowFileParseResp resp = null;
        for (String documentId : req.getDocumentIds()) {
            Document document = documentService.lambdaQuery()
                    .eq(Document::getId, documentId)
                    .one();
            
            if (document == null) {
                log.error("未找到指定的文档: {}", documentId);
                continue;
            }

            String datasetId = document.getKbId();

            if (datasetId != null) {
                resp = ragFlowFileAPIService.startParseTask(datasetId, req);
            }
        }
        return resp;
    }

    @Override
    public RAGFlowFileParseResp stopParseTask(RAGFlowFileParseReq req) {
        RAGFlowFileParseResp resp = null;
        for (String documentId : req.getDocumentIds()) {
            Document document = documentService.lambdaQuery()
                    .eq(Document::getId, documentId)
                    .one();
            
            if (document == null) {
                log.error("未找到指定的文档: {}", documentId);
                continue;
            }

            String datasetId = document.getId();

            if (datasetId != null) {
                resp = ragFlowFileAPIService.stopParseTask(datasetId, req);
            }
        }
        return resp;
    }

    @Override
    public RagFlowChunkListResp listChunks(String documentId, RagFlowChunkListReq req) {
        String datasetId = documentService.getDatasetId(documentId);
        return ragFlowFileChunkAPIService.listChunks(datasetId, documentId, req);
    }

    @Override
    public RagFlowChunkUpdateResp updateChunk(String chunkId, RagFlowChunkUpdateReq req) {
        return null;
    }
}
