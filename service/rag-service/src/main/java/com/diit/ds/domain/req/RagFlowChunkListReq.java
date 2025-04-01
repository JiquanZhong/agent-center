package com.diit.ds.domain.req;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * RAGFlow切片列表查询请求
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RagFlowChunkListReq {
    
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
     * 关键词
     */
    private String keywords;
    
    /**
     * 切片ID
     */
    private String id;
} 