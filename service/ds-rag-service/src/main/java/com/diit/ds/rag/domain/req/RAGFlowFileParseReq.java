package com.diit.ds.rag.domain.req;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * RAGFlow文件解析请求
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RAGFlowFileParseReq {
    /**
     * 要解析的文档ID列表
     */
    @JsonProperty("document_ids")
    private List<String> documentIds;
} 