package com.diit.ds.rag.domain.req;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.util.List;

/**
 * RAGFlow删除数据集请求DTO
 * {
 *   "ids": ["ccbb4b820d4b11f0a3190242ac150006"]
 * }
 */
@Data
@JsonInclude(JsonInclude.Include.NON_NULL)  // 不序列化null值
public class RAGFlowDatasetDeleteReq {
    /**
     * 要删除的数据集ID列表
     * 如果不指定，将删除所有数据集
     */
    @JsonProperty("ids")
    private List<String> ids;
} 