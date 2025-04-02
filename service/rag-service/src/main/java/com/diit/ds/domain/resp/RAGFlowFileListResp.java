package com.diit.ds.domain.resp;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;

/**
 * RAGFlow文件列表查询响应
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RAGFlowFileListResp {
    /**
     * 响应码
     */
    private Integer code;
    
    /**
     * 错误信息
     */
    private String message;
    
    /**
     * 文件列表数据
     */
    private FileListData data;
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class FileListData {
        /**
         * 文档列表
         */
        private List<FileInfo> docs;
        
        /**
         * 总数
         */
        private Integer total;
    }
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class FileInfo {
        /**
         * 切片数量
         */
        @JsonProperty("chunk_count")
        private Integer chunkCount;
        
        /**
         * 创建日期
         */
        @JsonProperty("create_date")
        private String createDate;
        
        /**
         * 创建时间戳
         */
        @JsonProperty("create_time")
        private Long createTime;
        
        /**
         * 创建者ID
         */
        @JsonProperty("created_by")
        private String createdBy;
        
        /**
         * 文档ID
         */
        private String id;
        
        /**
         * 知识库ID
         */
        @JsonProperty("knowledgebase_id")
        private String knowledgebaseId;
        
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
         * 切片方法
         */
        @JsonProperty("chunk_method")
        private String chunkMethod;
        
        /**
         * 处理开始时间
         */
        @JsonProperty("process_begin_at")
        private String processBeginAt;
        
        /**
         * 处理持续时间
         */
        @JsonProperty("process_duation")
        private Double processDuation;
        
        /**
         * 进度
         */
        private Double progress;
        
        /**
         * 进度消息
         */
        @JsonProperty("progress_msg")
        private String progressMsg;
        
        /**
         * 运行状态
         */
        private String run;
        
        /**
         * 文件大小
         */
        private Long size;
        
        /**
         * 来源类型
         */
        @JsonProperty("source_type")
        private String sourceType;
        
        /**
         * 状态
         */
        private String status;
        
        /**
         * 缩略图
         */
        private String thumbnail;
        
        /**
         * 令牌数量
         */
        @JsonProperty("token_count")
        private Integer tokenCount;
        
        /**
         * 文件类型
         */
        private String type;
        
        /**
         * 更新日期
         */
        @JsonProperty("update_date")
        private String updateDate;
        
        /**
         * 更新时间戳
         */
        @JsonProperty("update_time")
        private Long updateTime;
        
        /**
         * 上传人用户名
         * 不是RAGFlow原始字段，由系统添加
         */
        private String username;
    }
} 