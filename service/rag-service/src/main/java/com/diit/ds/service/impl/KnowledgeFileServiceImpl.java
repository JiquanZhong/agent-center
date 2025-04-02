package com.diit.ds.service.impl;

import com.baomidou.dynamic.datasource.annotation.DS;
import com.diit.ds.context.UserContext;
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
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.stream.Collectors;

/**
 * 知识库文件服务实现类
 * 负责处理知识库文件的上传、下载、列表查询、删除等操作
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class KnowledgeFileServiceImpl implements KnowledgeFileService {

    private final KnowledgeTreeNodeService knowledgeTreeNodeService;
    private final DocumentService documentService;
    private final RAGFlowFileAPIService ragFlowFileAPIService;
    private final RagFlowFileChunkAPIService ragFlowFileChunkAPIService;
    private final ExecutorService executorService;

    /**
     * 上传文件到指定知识库节点
     *
     * @param treeNodeId 知识库节点ID
     * @param files 要上传的文件数组
     * @return 上传结果响应
     */
    @Override
    public RAGFlowFileUploadResp uploadFiles(String treeNodeId, MultipartFile[] files) {
        return uploadFiles(treeNodeId, files, false);
    }
    
    /**
     * 上传文件到指定知识库节点，可选是否转换为PDF
     *
     * @param treeNodeId 知识库节点ID
     * @param files 要上传的文件数组
     * @param convertToPdf 是否将文件转换为PDF格式
     * @return 上传结果响应
     */
    @Override
    public RAGFlowFileUploadResp uploadFiles(String treeNodeId, MultipartFile[] files, boolean convertToPdf) {
        // 查询知识库节点信息
        KnowledgeTreeNode treeNode = knowledgeTreeNodeService.lambdaQuery()
                .eq(KnowledgeTreeNode::getId, treeNodeId)
                .one();
        
        // 检查节点是否存在
        if (treeNode == null) {
            log.error("未找到指定的知识库节点: {}", treeNodeId);
            RAGFlowFileUploadResp errorResp = new RAGFlowFileUploadResp();
            errorResp.setCode(404);
            errorResp.setMessage("未找到指定的知识库节点");
            return errorResp;
        }
        
        // 获取数据集ID
        String datasetId = treeNode.getKdbId();
        
        // 获取当前用户名
        String username = UserContext.getUserName();
        log.info("当前上传用户: {}", username);
        
        RAGFlowFileUploadResp uploadResp;
        
        // 检查是否需要转换为PDF
        if (!convertToPdf) {
            // 直接上传原始文件
            uploadResp = ragFlowFileAPIService.uploadFiles(datasetId, files);
        } else {
            // 转换为PDF后再上传
            List<CompletableFuture<MultipartFile>> futures = new ArrayList<>();
            
            // 遍历所有文件进行处理
            for (MultipartFile file : files) {
                if (file == null || file.isEmpty()) {
                    continue;
                }
                
                String originalFilename = file.getOriginalFilename();
                if (originalFilename == null) {
                    log.warn("跳过无法获取文件名的文件");
                    continue;
                }
                
                // 提交到线程池异步处理文件转换
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
            
            // 检查是否有可上传的文件
            if (pdfFiles.isEmpty()) {
                log.error("没有可以上传的文件");
                RAGFlowFileUploadResp errorResp = new RAGFlowFileUploadResp();
                errorResp.setCode(400);
                errorResp.setMessage("没有可以上传的文件");
                return errorResp;
            }
            
            log.info("所有文件转换完成，开始上传 {} 个文件", pdfFiles.size());
            
            // 上传到RAGFlow
            uploadResp = ragFlowFileAPIService.uploadFiles(datasetId, pdfFiles.toArray(new MultipartFile[0]));
        }
        
        // 上传文件成功后，更新对应的文档记录，添加用户名
        if (uploadResp != null && uploadResp.getCode() == 0 && uploadResp.getData() != null 
                && !uploadResp.getData().isEmpty() && username != null && !username.isEmpty()) {
            List<Document> documentsToUpdate = new ArrayList<>();
            
            // 遍历上传成功的文件，更新用户名
            for (RAGFlowFileUploadResp.FileInfo fileInfo : uploadResp.getData()) {
                try {
                    Document document = documentService.lambdaQuery()
                            .eq(Document::getId, fileInfo.getId())
                            .one();
                    
                    if (document != null) {
                        document.setUsername(username);
                        documentsToUpdate.add(document);
                        log.info("为文档 {} 设置用户名: {}", fileInfo.getId(), username);
                    } else {
                        log.warn("未找到文档记录: {}", fileInfo.getId());
                    }
                } catch (Exception e) {
                    log.error("获取文档记录失败: {}", e.getMessage(), e);
                }
            }
            
            // 批量更新文档记录
            if (!documentsToUpdate.isEmpty()) {
                try {
                    boolean updateResult = documentService.updateBatchById(documentsToUpdate);
                    log.debug("批量更新文档用户名结果: {}, 更新数量: {}", updateResult, documentsToUpdate.size());
                } catch (Exception e) {
                    log.error("批量更新文档用户名失败: {}", e.getMessage(), e);
                }
            }
        }
        
        return uploadResp;
    }

    /**
     * 下载指定文档
     *
     * @param documentId 文档ID
     * @param outputStream 输出流，用于写入文件内容
     * @return 文件名
     * @throws FileNotFoundException 当文件不存在时抛出异常
     */
    @Override
    public String downloadFile(String documentId, OutputStream outputStream) throws FileNotFoundException {
        // 查询文档信息
        Document document = documentService.lambdaQuery()
                .eq(Document::getId, documentId)
                .one();
        
        // 检查文档是否存在
        if (document == null) {
            throw new FileNotFoundException("文件不存在");
        }

        // 获取数据集ID
        String datasetId = document.getKbId();

        // 检查数据集ID是否存在
        if (datasetId == null) {
            throw new FileNotFoundException("文件不存在");
        }

        // 调用RAGFlow API下载文件
        boolean success = ragFlowFileAPIService.downloadFile(datasetId, documentId, outputStream);

        // 下载成功返回文件名，否则返回null
        if (success) {
            return document.getName();
        }

        return null;
    }

    /**
     * 获取指定知识库节点下的文件列表
     *
     * @param treeNodeId 知识库节点ID
     * @param req 列表查询请求参数
     * @return 文件列表响应
     */
    @Override
    public RAGFlowFileListResp listFiles(String treeNodeId, RAGFlowFileListReq req) {
        // 查询知识库节点信息
        KnowledgeTreeNode treeNode = knowledgeTreeNodeService.lambdaQuery()
                .eq(KnowledgeTreeNode::getId, treeNodeId)
                .one();
        
        // 检查节点是否存在
        if (treeNode == null) {
            log.error("未找到指定的知识库节点: {}", treeNodeId);
            RAGFlowFileListResp errorResp = new RAGFlowFileListResp();
            errorResp.setCode(404);
            errorResp.setMessage("未找到指定的知识库节点");
            return errorResp;
        }

        // 获取数据集ID并调用RAGFlow API获取文件列表
        String datasetId = treeNode.getKdbId();
        RAGFlowFileListResp resp = ragFlowFileAPIService.listFiles(datasetId, req);

        // 给文件列表添加用户名信息
        if (resp != null && resp.getCode() == 0 && resp.getData() != null && 
                resp.getData().getDocs() != null && !resp.getData().getDocs().isEmpty()) {
            
            // 收集所有文档ID
            List<String> documentIds = resp.getData().getDocs().stream()
                    .map(RAGFlowFileListResp.FileInfo::getId)
                    .collect(Collectors.toList());
            
            try {
                // 批量查询文档信息
                List<Document> documents = documentService.lambdaQuery()
                        .in(Document::getId, documentIds)
                        .list();
                
                // 创建ID到用户名的映射
                Map<String, String> documentIdToUsername = documents.stream()
                        .filter(doc -> doc.getUsername() != null && !doc.getUsername().isEmpty())
                        .collect(Collectors.toMap(Document::getId, Document::getUsername));
                
                // 将用户名信息添加到文件列表
                resp.getData().getDocs().forEach(fileInfo -> {
                    String username = documentIdToUsername.get(fileInfo.getId());
                    if (username != null) {
                        fileInfo.setUsername(username);
                    }
                });
                
                log.debug("文件列表添加用户名信息成功，处理文件数: {}", resp.getData().getDocs().size());
            } catch (Exception e) {
                log.error("给文件列表添加用户名信息失败: {}", e.getMessage(), e);
            }
        }

        return resp;
    }

    /**
     * 删除指定文件
     *
     * @param req 删除请求参数，包含要删除的文件ID列表
     * @return 删除结果响应
     */
    @Override
    public RAGFlowFileDeleteResp deleteFiles(RAGFlowFileDeleteReq req) {
        RAGFlowFileDeleteResp resp = null;
        // 遍历要删除的文档ID
        for (String documentId : req.getIds()) {
            // 查询文档信息
            Document document = documentService.lambdaQuery()
                    .eq(Document::getId, documentId)
                    .one();
            
            // 检查文档是否存在
            if (document == null) {
                log.error("未找到指定的文档: {}", documentId);
                continue;
            }

            // 获取数据集ID
            String datasetId = document.getId();

            // 调用RAGFlow API删除文件
            if (datasetId != null) {
                resp = ragFlowFileAPIService.deleteFiles(datasetId, req);
            }
        }
        return resp;
    }

    /**
     * 启动文件解析任务
     *
     * @param req 解析请求参数，包含要解析的文档ID列表
     * @return 解析任务响应
     */
    @Override
    public RAGFlowFileParseResp startParseTask(RAGFlowFileParseReq req) {
        RAGFlowFileParseResp resp = null;
        // 遍历要解析的文档ID
        for (String documentId : req.getDocumentIds()) {
            // 查询文档信息
            Document document = documentService.lambdaQuery()
                    .eq(Document::getId, documentId)
                    .one();
            
            // 检查文档是否存在
            if (document == null) {
                log.error("未找到指定的文档: {}", documentId);
                continue;
            }

            // 获取数据集ID
            String datasetId = document.getKbId();

            // 调用RAGFlow API启动解析任务
            if (datasetId != null) {
                resp = ragFlowFileAPIService.startParseTask(datasetId, req);
            }
        }
        return resp;
    }

    /**
     * 停止文件解析任务
     *
     * @param req 解析请求参数，包含要停止解析的文档ID列表
     * @return 解析任务响应
     */
    @Override
    public RAGFlowFileParseResp stopParseTask(RAGFlowFileParseReq req) {
        RAGFlowFileParseResp resp = null;
        // 遍历要停止解析的文档ID
        for (String documentId : req.getDocumentIds()) {
            // 查询文档信息
            Document document = documentService.lambdaQuery()
                    .eq(Document::getId, documentId)
                    .one();
            
            // 检查文档是否存在
            if (document == null) {
                log.error("未找到指定的文档: {}", documentId);
                continue;
            }

            // 获取数据集ID
            String datasetId = document.getId();

            // 调用RAGFlow API停止解析任务
            if (datasetId != null) {
                resp = ragFlowFileAPIService.stopParseTask(datasetId, req);
            }
        }
        return resp;
    }

    /**
     * 获取文档的分块列表
     *
     * @param documentId 文档ID
     * @param req 分块列表请求参数
     * @return 分块列表响应
     */
    @Override
    public RagFlowChunkListResp listChunks(String documentId, RagFlowChunkListReq req) {
        // 获取数据集ID
        String datasetId = documentService.getDatasetId(documentId);
        // 调用RAGFlow API获取分块列表
        return ragFlowFileChunkAPIService.listChunks(datasetId, documentId, req);
    }

    /**
     * 更新文档分块内容
     *
     * @param chunkId 分块ID
     * @param req 分块更新请求参数
     * @return 分块更新响应
     */
    @Override
    public RagFlowChunkUpdateResp updateChunk(String chunkId, RagFlowChunkUpdateReq req) {
        return null;
    }

    /**
     * 字节数组形式的MultipartFile实现
     * 用于将内存中的字节数组包装成MultipartFile对象
     */
    private static class ByteArrayMultipartFile implements MultipartFile {
        private final byte[] content;
        private final String filename;
        
        /**
         * 构造函数
         *
         * @param content 文件内容字节数组
         * @param filename 文件名
         */
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
