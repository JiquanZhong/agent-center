package com.diit.ds.rag.domain.req;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * RAGFlow文件删除请求
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RAGFlowFileDeleteReq {
    /**
     * 要删除的文档ID列表
     */
    private List<String> ids;
} 