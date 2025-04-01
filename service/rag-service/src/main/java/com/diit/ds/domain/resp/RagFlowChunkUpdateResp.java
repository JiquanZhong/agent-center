package com.diit.ds.domain.resp;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * RAGFlow切片更新响应
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RagFlowChunkUpdateResp {
    
    /**
     * 响应码，0表示成功
     */
    private Integer code;
    
    /**
     * 错误信息
     */
    private String message;
} 