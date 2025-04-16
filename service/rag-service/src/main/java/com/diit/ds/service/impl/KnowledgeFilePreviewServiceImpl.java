package com.diit.ds.service.impl;

import com.diit.ds.domain.dto.KnowledgeSearchResultDTO;
import com.diit.ds.domain.dto.WorkflowChunkDTO;
import com.diit.ds.domain.dto.WorkflowDocAggDTO;
import com.diit.ds.domain.entity.Document;
import com.diit.ds.domain.entity.WorkflowChunk;
import com.diit.ds.domain.entity.WorkflowDocAgg;
import com.diit.ds.domain.resp.FilePreviewResp;
import com.diit.ds.exception.FileNotFoundException;
import com.diit.ds.service.*;
import com.diit.ds.structmapper.WorkflowChunkSM;
import com.diit.ds.structmapper.WorkflowDocAggSM;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class KnowledgeFilePreviewServiceImpl implements KnowledgeFilePreviewService, InitializingBean {

    private final KnowledgeFileService knowledgeFileService;
    private final DocumentService documentService;
    private final WorkflowRunService workflowRunService;
    private final WorkflowChunkService workflowChunkService;
    private final WorkflowDocAggService workflowDocAggService;
    
    @Value("${file.preview-path:/app/preview/}")
    private String previewPath;
    
    @Value("${file.preview-mapping:/preview/}")
    private String previewMapping;

    // DIOS文件预览URL配置
    @Value("${diit.dios.api.url}")
    private String diosUrl;

    private String diosFilePreviewUrlPrefix;
    private String diosFilePreviewUrlSuffix = "/file-preview";
    
    @Override
    public void afterPropertiesSet() throws Exception {
        diosFilePreviewUrlPrefix = diosUrl + "/data-center/#/overview/resource/info-view/data/";
        log.info("DIOS文件预览URL前缀初始化完成: {}", diosFilePreviewUrlPrefix);
    }

    /**
     * 获取文件预览URL
     * 
     * 该方法负责将文档下载到预览目录，并返回Nginx可访问的URL
     *
     * @param documentId 文档ID
     * @return 文件预览响应，包含预览URL
     * @throws FileNotFoundException 文件不存在时抛出异常
     */
    @Override
    public FilePreviewResp previewFile(String documentId) throws FileNotFoundException {
        // 检查文档是否存在
        Document document = documentService.lambdaQuery()
                .eq(Document::getId, documentId)
                .one();
        
        if (document == null) {
            log.error("未找到指定的文档: {}", documentId);
            throw new FileNotFoundException("文件不存在");
        }
        
        // 获取文件名称
        String fileName = document.getName();
        String safeFileName = documentId + "_" + fileName;
        
        // 创建预览目录
        createPreviewDirectory();
        
        // 构建文件保存路径
        File previewFile = new File(previewPath, safeFileName);
        
        // 如果文件已存在，直接返回URL（避免重复下载）
        // if (previewFile.exists()) {
        //     log.info("文件已存在，直接返回预览URL: {}", previewFile.getAbsolutePath());
        //     return buildPreviewResponse(documentId, fileName, safeFileName);
        // }
        
        try (FileOutputStream outputStream = new FileOutputStream(previewFile)) {
            // 调用KnowledgeFileService下载文件到预览目录
            String downloadedFileName = knowledgeFileService.downloadFile(documentId, outputStream);
            
            if (downloadedFileName == null) {
                log.error("文件下载失败: {}", documentId);
                throw new FileNotFoundException("文件下载失败");
            }
            
            log.info("文件下载成功: {}, 保存路径: {}", safeFileName, previewFile.getAbsolutePath());
            
            return buildPreviewResponse(documentId, fileName, safeFileName);
        } catch (IOException e) {
            log.error("文件预览处理异常: {}", e.getMessage(), e);
            throw new FileNotFoundException("文件预览处理异常: " + e.getMessage());
        }
    }

    @Override
    public KnowledgeSearchResultDTO getKnowledgeQueryInfo(String workFlowRunId) {
        // 根据work_flow_run_id获取知识查询的相关信息
        KnowledgeSearchResultDTO resultDTO = new KnowledgeSearchResultDTO();
        resultDTO.setWorkFlowRunId(workFlowRunId);
        
        // 1. 获取查询片段数据
        List<WorkflowChunk> chunks = workflowChunkService.lambdaQuery()
                .eq(WorkflowChunk::getWorkFlowRunId, workFlowRunId)
                .orderByAsc(WorkflowChunk::getIndex)
                .list();
        
        // 2. 获取文档聚合数据
        List<WorkflowDocAgg> docAggs = workflowDocAggService.lambdaQuery()
                .eq(WorkflowDocAgg::getWorkFlowRunId, workFlowRunId)
                .orderByDesc(WorkflowDocAgg::getCount)
                .list();
        
        // 如果没有查询结果，直接返回空数据
        if (CollectionUtils.isEmpty(chunks) && CollectionUtils.isEmpty(docAggs)) {
            log.warn("未找到workFlowRunId为{}的知识查询记录", workFlowRunId);
            resultDTO.setWorkflowChunks(Collections.emptyList());
            resultDTO.setWorkflowChunksFiles(Collections.emptyList());
            return resultDTO;
        }
        
        // 3. 使用MapStruct将实体转换为DTO
        List<WorkflowChunkDTO> chunkDTOs = new ArrayList<>();
        Map<String, String> docPreviewUrlMap = null;
        if (!CollectionUtils.isEmpty(chunks)) {
            // 提取所有文档ID
            Set<String> documentIds = chunks.stream()
                    .map(WorkflowChunk::getDocumentId)
                    .filter(Objects::nonNull)
                    .collect(Collectors.toSet());
            
            // 生成文档预览URL映射
            docPreviewUrlMap = generatePreviewUrls(documentIds);
            
            // 转换并设置预览URL
            for (WorkflowChunk chunk : chunks) {
                WorkflowChunkDTO chunkDTO = WorkflowChunkSM.INSTANCE.toDTO(chunk);
                
                // 设置预览URL
                if (chunk.getDocumentId() != null && docPreviewUrlMap.containsKey(chunk.getDocumentId())) {
                    chunkDTO.setPreviewUrl(docPreviewUrlMap.get(chunk.getDocumentId()));
                }
                
                chunkDTOs.add(chunkDTO);
            }
        }
        
        // 4. 处理文档聚合数据
        List<WorkflowDocAggDTO> docAggDTOs = new ArrayList<>();
        if (!CollectionUtils.isEmpty(docAggs)) {
            
            // 转换并设置预览URL
            for (WorkflowDocAgg docAgg : docAggs) {
                WorkflowDocAggDTO docAggDTO = WorkflowDocAggSM.INSTANCE.toDTO(docAgg);
                
                // 设置预览URL
                if (docAgg.getDocId() != null && docPreviewUrlMap.containsKey(docAgg.getDocId())) {
                    docAggDTO.setPreviewUrl(docPreviewUrlMap.get(docAgg.getDocId()));
                }

                docAggDTO.setFileType(docAggDTO.getDocName().substring(docAggDTO.getDocName().lastIndexOf(".") + 1));
                
                docAggDTOs.add(docAggDTO);
            }
        }
        
        resultDTO.setWorkflowChunks(chunkDTOs);
        resultDTO.setWorkflowChunksFiles(docAggDTOs);
        
        return resultDTO;
    }
    
    /**
     * 为多个文档生成预览URL，确保重复文件只生成一次URL
     * 
     * @param documentIds 文档ID集合
     * @return 文档ID -> 预览URL的映射
     */
    private Map<String, String> generatePreviewUrls(Set<String> documentIds) {
        Map<String, String> docPreviewUrlMap = new HashMap<>();
        
        if (CollectionUtils.isEmpty(documentIds)) {
            return docPreviewUrlMap;
        }
        
        // 批量查询文档信息
        List<Document> documents = documentService.lambdaQuery()
                .in(Document::getId, documentIds)
                .list();
        
        if (CollectionUtils.isEmpty(documents)) {
            // 如果没找到，说明是DIOS的资源
            // 直接返回前缀+文档ID+后缀的URL
            for (String documentId : documentIds) {
                String previewUrl = diosFilePreviewUrlPrefix + documentId + diosFilePreviewUrlSuffix;
                docPreviewUrlMap.put(documentId, previewUrl);
            }

            return docPreviewUrlMap;
        }
        
        // 创建预览目录
        createPreviewDirectory();
        
        // 为每个文档生成预览URL
        for (Document document : documents) {
            String documentId = document.getId();
            String fileName = document.getName();
            String safeFileName = documentId + "_" + fileName;
            
            // 构建文件保存路径
            File previewFile = new File(previewPath, safeFileName);
            
            try {
                // 如果文件已存在，直接获取URL（避免重复下载）
//                if (previewFile.exists()) {
//                    log.info("文件已存在，直接返回预览URL: {}", previewFile.getAbsolutePath());
//                    String previewUrl = previewMapping + safeFileName;
//                    docPreviewUrlMap.put(documentId, previewUrl);
//                    continue;
//                }
                
                // 下载文件并生成URL
                try (FileOutputStream outputStream = new FileOutputStream(previewFile)) {
                    // 调用KnowledgeFileService下载文件到预览目录
                    String downloadedFileName = knowledgeFileService.downloadFile(documentId, outputStream);
                    
                    if (downloadedFileName == null) {
                        log.error("文件下载失败: {}", documentId);
                        continue;
                    }
                    
                    log.info("文件下载成功: {}, 保存路径: {}", safeFileName, previewFile.getAbsolutePath());
                    
                    // 构建预览URL
                    String previewUrl = previewMapping + safeFileName;
                    docPreviewUrlMap.put(documentId, previewUrl);
                }
            } catch (IOException e) {
                log.error("文件预览处理异常: {}", e.getMessage(), e);
            }
        }
        
        return docPreviewUrlMap;
    }

    /**
     * 构建预览响应
     */
    private FilePreviewResp buildPreviewResponse(String documentId, String fileName, String safeFileName) {
        // 构建由Nginx代理的预览URL
        String previewUrl = previewMapping + safeFileName;
        
        // 获取文件类型
        String fileType = getFileType(fileName);
        
        // 构建响应对象
        FilePreviewResp resp = new FilePreviewResp();
        resp.setDocumentId(documentId);
        resp.setFileName(fileName);
        resp.setPreviewUrl(previewUrl);
        resp.setFileType(fileType);
        
        return resp;
    }
    
    /**
     * 创建预览文件目录
     */
    private void createPreviewDirectory() {
        File directory = new File(previewPath);
        if (!directory.exists()) {
            boolean created = directory.mkdirs();
            if (created) {
                log.info("创建预览目录成功: {}", directory.getAbsolutePath());
            } else {
                log.warn("创建预览目录失败: {}", directory.getAbsolutePath());
            }
        }
    }
    
    /**
     * 获取文件类型
     * 
     * @param fileName 文件名
     * @return 文件类型
     */
    private String getFileType(String fileName) {
        String extension = "";
        int i = fileName.lastIndexOf('.');
        if (i > 0) {
            extension = fileName.substring(i + 1).toLowerCase();
        }
        return extension;
    }
}
