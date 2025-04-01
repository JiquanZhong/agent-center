package com.diit.ds.domain.resp;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;

/**
 * RAGFlow文件上传响应
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RAGFlowFileUploadResp {
    /**
     * 响应码
     */
    private Integer code;
    
    /**
     * 错误信息
     */
    private String message;
    
    /**
     * 上传的文件信息列表
     */
    private List<FileInfo> data;
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class FileInfo {
        /**
         * 切片方法
         */
        @JsonProperty("chunk_method")
        private String chunkMethod;
        
        /**
         * 创建者ID
         */
        @JsonProperty("created_by")
        private String createdBy;
        
        /**
         * 数据集ID
         */
        @JsonProperty("dataset_id")
        private String datasetId;
        
        /**
         * 文档ID
         */
        private String id;
        
        /**
         * 文件存储位置
         */
        private String location;
        
        /**
         * 文件名称
         */
        private String name;
        
        /**
         * 解析配置
         */
        @JsonProperty("parser_config")
        private Map<String, Object> parserConfig;
        
        /**
         * 运行状态
         */
        private String run;
        
        /**
         * 文件大小
         */
        private Long size;
        
        /**
         * 缩略图
         */
        private String thumbnail;
        
        /**
         * 文件类型
         */
        private String type;
    }
} 