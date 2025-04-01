package com.diit.ds.domain.req;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * RAGFlow切片更新请求
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RagFlowChunkUpdateReq {

    /**
     * 切片内容
     */
    private String content;
    
    /**
     * 重要关键词
     */
    @JsonProperty("important_keywords")
    private List<String> importantKeywords;
} 