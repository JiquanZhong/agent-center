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
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.HashMap;
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
    private final ObjectMapper objectMapper = new ObjectMapper();

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
                        if (ext.equals("pdf") || ext.equals("xlsx")) {
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

        // 更新节点及其父节点的文档数量
        try {
            // 增加的文档数量
            int addedDocumentCount = uploadResp.getData().size();
            // 更新当前节点及其父节点的文档数量
            knowledgeTreeNodeService.updateNodeAndParentsDocumentNum(treeNodeId, addedDocumentCount);
            log.info("已更新节点[{}]及其父节点的文档数量，增加文档数: {}", treeNodeId, addedDocumentCount);
        } catch (Exception e) {
            log.error("更新节点文档数量失败: {}", e.getMessage(), e);
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
        if (!treeNodeId.equals("0")) {
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
        }

        // 获取数据集ID
//        String datasetId = treeNode.getKdbId();
        List<String> kbIds = knowledgeTreeNodeService.getKbIdsByPid(treeNodeId);
        
        // 创建响应对象
        RAGFlowFileListResp resp = new RAGFlowFileListResp();
        resp.setCode(0);
        resp.setMessage(null);
        
        // 构建查询条件
        com.baomidou.mybatisplus.extension.plugins.pagination.Page<Document> page = 
            new com.baomidou.mybatisplus.extension.plugins.pagination.Page<>(
                req.getPage() != null ? req.getPage() : 1, 
                req.getPageSize() != null ? req.getPageSize() : 10
            );
        
        // 创建查询条件构造器
        com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<Document> queryWrapper = 
            new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<>();
        
        // 设置数据集ID条件
        queryWrapper.in(Document::getKbId, kbIds);
        
        // 添加其他查询条件
        if (req.getId() != null && !req.getId().isEmpty()) {
            queryWrapper.eq(Document::getId, req.getId());
        }
        
        if (req.getName() != null && !req.getName().isEmpty()) {
            queryWrapper.like(Document::getName, req.getName());
        }
        
        if (req.getStatus() != null && !req.getStatus().isEmpty()) {
            String runCode = null;
            switch (req.getStatus()) {
                case "未解析":
                    runCode = "0";
                    break;
                case "解析中":
                    runCode = "1";
                    break;
                case "已入库":
                    runCode = "3";
                    break;
                case "已取消":
                    runCode = "2";
                    break;
                case "解析失败":
                    runCode = "4";
                    break;
            }
            //非以上条件，则不进行筛选
            if (runCode != null) {
                queryWrapper.eq(Document::getRun, runCode);
            }
        }
        
        if (req.getKeywords() != null && !req.getKeywords().isEmpty()) {
            queryWrapper.and(wrapper -> {
                wrapper.like(Document::getName, req.getKeywords())
                       .or()
                       .like(Document::getLocation, req.getKeywords());
            });
        }
        
        // 设置排序
        if (req.getOrderby() != null && !req.getOrderby().isEmpty()) {
            String orderBy = req.getOrderby();
            boolean isDesc = req.getDesc() != null && req.getDesc();
            
            switch (orderBy) {
                case "create_time":
                    if (isDesc) {
                        queryWrapper.orderByDesc(Document::getCreateTime);
                    } else {
                        queryWrapper.orderByAsc(Document::getCreateTime);
                    }
                    break;
                case "update_time":
                    if (isDesc) {
                        queryWrapper.orderByDesc(Document::getUpdateTime);
                    } else {
                        queryWrapper.orderByAsc(Document::getUpdateTime);
                    }
                    break;
                default:
                    queryWrapper.orderByDesc(Document::getCreateTime);
                    break;
            }
        } else {
            queryWrapper.orderByDesc(Document::getCreateTime);
        }
        
        // 执行分页查询
        com.baomidou.mybatisplus.extension.plugins.pagination.Page<Document> result = 
            documentService.page(page, queryWrapper);
        
        // 构造响应数据
        RAGFlowFileListResp.FileListData fileListData = new RAGFlowFileListResp.FileListData();
        fileListData.setTotal((int) result.getTotal());
        
        List<RAGFlowFileListResp.FileInfo> fileInfoList = new ArrayList<>();
        
        // 转换Document实体到FileInfo
        for (Document document : result.getRecords()) {
            RAGFlowFileListResp.FileInfo fileInfo = convertDocumentToFileInfo(document);
            fileInfoList.add(fileInfo);
        }
        
        fileListData.setDocs(fileInfoList);
        resp.setData(fileListData);
        
        return resp;
    }
    
    /**
     * 将Document实体转换为FileInfo对象
     * 
     * @param document 文档实体
     * @return FileInfo对象
     */
    private RAGFlowFileListResp.FileInfo convertDocumentToFileInfo(Document document) {
        RAGFlowFileListResp.FileInfo fileInfo = new RAGFlowFileListResp.FileInfo();
        
        fileInfo.setId(document.getId());
        fileInfo.setName(document.getName());
        fileInfo.setLocation(document.getLocation());
        fileInfo.setSize(document.getSize().longValue());
        fileInfo.setType(document.getType());
        fileInfo.setThumbnail(document.getThumbnail());
        fileInfo.setCreatedBy(document.getCreatedBy());
        fileInfo.setUsername(document.getUsername());
        fileInfo.setStatus(document.getStatus());
        
        // 解析JSON格式的Parser Config
        try {
            if (document.getParserConfig() != null && !document.getParserConfig().isEmpty()) {
                Map<String, Object> parserConfig = objectMapper.readValue(
                    document.getParserConfig(), 
                    new TypeReference<Map<String, Object>>() {}
                );
                fileInfo.setParserConfig(parserConfig);
            }
        } catch (Exception e) {
            log.error("解析Parser Config失败: {}", e.getMessage(), e);
        }
        
        fileInfo.setCreateTime(document.getCreateTime());
        fileInfo.setUpdateTime(document.getUpdateTime());
        
        // 转换日期格式为标准GMT格式
        if (document.getCreateDate() != null) {
            String createDateStr = new java.text.SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss zzz", java.util.Locale.US)
                    .format(document.getCreateDate());
            fileInfo.setCreateDate(createDateStr);
        }
        
        if (document.getUpdateDate() != null) {
            String updateDateStr = new java.text.SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss zzz", java.util.Locale.US)
                    .format(document.getUpdateDate());
            fileInfo.setUpdateDate(updateDateStr);
        }
        
        fileInfo.setProgress(document.getProgress());
        
        // 将数字状态转换为文本状态
        if (document.getRun() != null) {
            switch (document.getRun()) {
                case "0":
                    fileInfo.setRun("UNSTART");
                    break;
                case "1":
                    fileInfo.setRun("RUNNING");
                    break;
                case "2":
                    fileInfo.setRun("CANCEL");
                    break;
                case "3":
                    fileInfo.setRun("DONE");
                    break;
                case "4":
                    fileInfo.setRun("FAIL");
                    break;
                default:
                    fileInfo.setRun(document.getRun());
                    break;
            }
        }
        
        fileInfo.setProgressMsg(document.getProgressMsg());
        fileInfo.setSourceType(document.getSourceType());
        
        // 将chunkNum映射到chunkCount
        fileInfo.setChunkCount(document.getChunkNum());
        
        // 将tokenNum映射到tokenCount
        fileInfo.setTokenCount(document.getTokenNum());
        
        // 处理处理时间，格式化为GMT格式
        if (document.getProcessBeginAt() != null) {
            String processBeginAtStr = new java.text.SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss zzz", java.util.Locale.US)
                    .format(document.getProcessBeginAt());
            fileInfo.setProcessBeginAt(processBeginAtStr);
        }
        
        fileInfo.setProcessDuation(document.getProcessDuation());
        
        // 知识库ID
        fileInfo.setKnowledgebaseId(document.getKbId());
        
        // 设置分块方法为naive
        fileInfo.setChunkMethod("naive");
        
        return fileInfo;
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
        
        // 用于记录每个节点需要减少的文档数量
        Map<String, Integer> nodeDocumentCountMap = new HashMap<>();
        
        // 先获取每个文档对应的知识树节点ID，用于后续更新文档数量
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
            String datasetId = document.getKbId();
            if (datasetId == null) {
                continue;
            }
            
            // 找到对应的知识树节点
            KnowledgeTreeNode treeNode = knowledgeTreeNodeService.lambdaQuery()
                    .eq(KnowledgeTreeNode::getKdbId, datasetId)
                    .one();
            
            if (treeNode != null) {
                // 记录该节点需要减少的文档数量
                String nodeId = treeNode.getId();
                nodeDocumentCountMap.put(nodeId, nodeDocumentCountMap.getOrDefault(nodeId, 0) + 1);
            }
            
            // 调用RAGFlow API删除文件
            if (datasetId != null) {
                resp = ragFlowFileAPIService.deleteFiles(datasetId, req);
            }
        }
        
        // 更新各节点及其父节点的文档数量
        for (Map.Entry<String, Integer> entry : nodeDocumentCountMap.entrySet()) {
            String nodeId = entry.getKey();
            Integer count = entry.getValue();
            
            try {
                // 更新当前节点及其父节点的文档数量（减少）
                knowledgeTreeNodeService.updateNodeAndParentsDocumentNum(nodeId, -count);
                log.info("已更新节点[{}]及其父节点的文档数量，减少文档数: {}", nodeId, count);
            } catch (Exception e) {
                log.error("更新节点[{}]文档数量失败: {}", nodeId, e.getMessage(), e);
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
            String datasetId = document.getKbId();

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
