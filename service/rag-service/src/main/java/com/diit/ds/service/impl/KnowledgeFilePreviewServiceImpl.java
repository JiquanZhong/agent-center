package com.diit.ds.service.impl;

import com.diit.ds.domain.entity.Document;
import com.diit.ds.domain.resp.FilePreviewResp;
import com.diit.ds.exception.FileNotFoundException;
import com.diit.ds.service.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

@Slf4j
@Service
@RequiredArgsConstructor
public class KnowledgeFilePreviewServiceImpl implements KnowledgeFilePreviewService {

    private final KnowledgeFileService knowledgeFileService;
    private final DocumentService documentService;
    
    @Value("${file.preview-path:/app/preview/}")
    private String previewPath;
    
    @Value("${file.preview-mapping:/preview/}")
    private String previewMapping;

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
