package com.diit.ds.domain.resp;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * RAGFlow文件删除响应
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RAGFlowFileDeleteResp {
    /**
     * 响应码，0表示成功
     */
    private Integer code;
    
    /**
     * 错误信息
     */
    private String message;
} 