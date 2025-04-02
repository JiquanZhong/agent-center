package com.diit.ds.service.impl;

import com.baomidou.dynamic.datasource.annotation.DS;
import com.diit.ds.domain.entity.Document;
import com.diit.ds.domain.entity.KnowledgeTreeNode;
import com.diit.ds.domain.req.*;
import com.diit.ds.domain.resp.*;
import com.diit.ds.exception.FileNotFoundException;
import com.diit.ds.service.*;
import com.diit.ds.util.PdfUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class KnowledgeFileServiceImpl implements KnowledgeFileService {

    private final KnowledgeTreeNodeService knowledgeTreeNodeService;
    private final DocumentService documentService;
    private final RAGFlowFileAPIService ragFlowFileAPIService;
    private final RagFlowFileChunkAPIService ragFlowFileChunkAPIService;
    private final ExecutorService executorService;

    @Override
    public RAGFlowFileUploadResp uploadFiles(String treeNodeId, MultipartFile[] files) {
        return uploadFiles(treeNodeId, files, false);
    }
    
    @Override
    public RAGFlowFileUploadResp uploadFiles(String treeNodeId, MultipartFile[] files, boolean convertToPdf) {
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
        
        // 检查是否需要转换为PDF
        if (!convertToPdf) {
            return ragFlowFileAPIService.uploadFiles(datasetId, files);
        }
        
        // 使用线程池并行处理文件转换
        List<CompletableFuture<MultipartFile>> futures = new ArrayList<>();
        
        for (MultipartFile file : files) {
            if (file == null || file.isEmpty()) {
                continue;
            }
            
            String originalFilename = file.getOriginalFilename();
            if (originalFilename == null) {
                log.warn("跳过无法获取文件名的文件");
                continue;
            }
            
            // 提交到线程池处理
            CompletableFuture<MultipartFile> future = CompletableFuture.supplyAsync(() -> {
                try {
                    String ext = originalFilename.substring(originalFilename.lastIndexOf(".") + 1).toLowerCase();
                    
                    // 如果已经是PDF，直接返回
                    if (ext.equals("pdf")) {
                        return file;
                    }
                    
                    // 转换为PDF
                    log.info("开始转换文件: {}", originalFilename);
                    ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
                    boolean success = PdfUtil.convertToPdf(file, outputStream);
                    
                    if (!success) {
                        log.error("文件转换失败：{}", originalFilename);
                        return null;
                    }
                    
                    // 创建转换后的PDF文件
                    String pdfFileName = originalFilename.substring(0, originalFilename.lastIndexOf(".")) + ".pdf";
                    byte[] pdfBytes = outputStream.toByteArray();
                    
                    log.info("文件转换完成: {} -> {}, 大小: {} 字节", originalFilename, pdfFileName, pdfBytes.length);
                    
                    // 创建一个MultipartFile对象
                    return new ByteArrayMultipartFile(pdfBytes, pdfFileName);
                } catch (Exception e) {
                    log.error("文件处理异常: {}, 错误: {}", originalFilename, e.getMessage(), e);
                    return null;
                }
            }, executorService);
            
            futures.add(future);
        }
        
        // 等待所有转换任务完成
        List<MultipartFile> pdfFiles = futures.stream()
                .map(CompletableFuture::join)  // 等待所有任务完成
                .filter(file -> file != null)   // 过滤掉处理失败的文件
                .collect(Collectors.toList());
        
        if (pdfFiles.isEmpty()) {
            log.error("没有可以上传的文件");
            RAGFlowFileUploadResp errorResp = new RAGFlowFileUploadResp();
            errorResp.setCode(400);
            errorResp.setMessage("没有可以上传的文件");
            return errorResp;
        }
        
        log.info("所有文件转换完成，开始上传 {} 个文件", pdfFiles.size());
        
        // 上传到RAGFlow
        return ragFlowFileAPIService.uploadFiles(datasetId, pdfFiles.toArray(new MultipartFile[0]));
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

    /**
     * 字节数组形式的MultipartFile实现
     */
    private static class ByteArrayMultipartFile implements MultipartFile {
        private final byte[] content;
        private final String filename;
        
        public ByteArrayMultipartFile(byte[] content, String filename) {
            this.content = content;
            this.filename = filename;
        }
        
        @Override
        public String getName() {
            return filename;
        }
        
        @Override
        public String getOriginalFilename() {
            return filename;
        }
        
        @Override
        public String getContentType() {
            return "application/pdf";
        }
        
        @Override
        public boolean isEmpty() {
            return content == null || content.length == 0;
        }
        
        @Override
        public long getSize() {
            return content.length;
        }
        
        @Override
        public byte[] getBytes() throws IOException {
            return content;
        }
        
        @Override
        public java.io.InputStream getInputStream() throws IOException {
            return new java.io.ByteArrayInputStream(content);
        }
        
        @Override
        public void transferTo(java.io.File dest) throws IOException, IllegalStateException {
            try (java.io.FileOutputStream fileOutputStream = new java.io.FileOutputStream(dest)) {
                fileOutputStream.write(content);
            }
        }
    }
}
