package com.diit.ds.rag.domain.req;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * H3C知识库查询接口的请求DTO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class H3CKnowledgeReq {
    /**
     * 知识库代码列表
     */
    @JsonProperty("knowledgeCodeList")
    private List<String> knowledgeCodeList;

    /**
     * 源问题
     */
    @JsonProperty("sourceQuestion")
    private String sourceQuestion;

    /**
     * 问题类型
     */
    @JsonProperty("questionType")
    private String questionType;

    /**
     * 问题列表
     */
    @JsonProperty("questionList")
    private List<String> questionList;

    /**
     * 返回结果数量
     */
    @JsonProperty("topK")
    private Integer topK;
} 