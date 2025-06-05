package com.diit.ds.rag.domain.resp;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * RAGFlow文件解析响应
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RAGFlowFileParseResp {
    /**
     * 响应码，0表示成功
     */
    private Integer code;
    
    /**
     * 错误信息
     */
    private String message;
} 