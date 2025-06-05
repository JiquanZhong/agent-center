package com.diit.ds.rag.domain.req;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

/**
 * Dios检索请求类
 */
@Data
public class DiosRetrieveReq {
    
    /**
     * 查询内容
     */
    private String query;
    
    /**
     * 返回的最大结果数量
     */
    @JsonProperty("top_k")
    private String topK;
    
    /**
     * 相似度阈值
     */
    @JsonProperty("score_threshold")
    private double scoreThreshold;
} 