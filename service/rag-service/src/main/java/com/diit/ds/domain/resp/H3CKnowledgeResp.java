package com.diit.ds.domain.resp;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * H3C知识库查询接口的响应DTO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class H3CKnowledgeResp {
    /**
     * 响应状态码
     */
    @JsonProperty("code")
    private Integer code;

    /**
     * 响应消息
     */
    @JsonProperty("msg")
    private String msg;

    /**
     * 响应数据
     */
    @JsonProperty("data")
    private List<ResultData> data;

    /**
     * 返回的知识条目数据
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ResultData {
        /**
         * 条目ID
         */
        @JsonProperty("id")
        private String id;

        /**
         * 文件名
         */
        @JsonProperty("fileName")
        private String fileName;

        /**
         * 内容
         */
        @JsonProperty("content")
        private String content;

        /**
         * 匹配得分
         */
        @JsonProperty("score")
        private Double score;
    }
} 