package com.diit.ds.domain.req;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * RAGFlow文件列表查询请求
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RAGFlowFileListReq {
    /**
     * 当前页码
     */
    private Integer page;
    
    /**
     * 每页数量
     */
    @JsonProperty("page_size")
    private Integer pageSize;
    
    /**
     * 排序字段
     */
    private String orderby;
    
    /**
     * 是否降序
     */
    private Boolean desc;
    
    /**
     * 关键词
     */
    private String keywords;
    
    /**
     * 文档ID
     */
    private String id;
    
    /**
     * 文档名称
     */
    private String name;
} 