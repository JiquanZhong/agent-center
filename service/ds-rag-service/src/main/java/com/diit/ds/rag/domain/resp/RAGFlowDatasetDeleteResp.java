package com.diit.ds.rag.domain.resp;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

/**
 * RAGFlow删除数据集响应DTO
 * {
 *   "code": 0,
 *   "message": null
 * }
 */
@Data
public class RAGFlowDatasetDeleteResp {
    /**
     * 响应码，0表示成功
     * 成功：code = 0
     * 失败：code = 102, message = "You don't own the dataset."
     */
    @JsonProperty("code")
    private int code;

    /**
     * 响应消息，成功时为null，失败时包含错误信息
     */
    @JsonProperty("message")
    private String message;
} 