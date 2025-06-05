package com.diit.ds.rag.domain.resp;

import lombok.Data;

/**
 * 文件预览响应DTO
 */
@Data
public class FilePreviewResp {
    
    /**
     * 文件ID
     */
    private String documentId;
    
    /**
     * 文件名称
     */
    private String fileName;
    
    /**
     * 预览URL
     */
    private String previewUrl;
    
    /**
     * 文件类型
     */
    private String fileType;
} 