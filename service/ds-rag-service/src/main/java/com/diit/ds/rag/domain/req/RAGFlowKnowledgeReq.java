package com.diit.ds.rag.domain.req;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.util.List;

@Data
@JsonInclude(JsonInclude.Include.NON_NULL)  // 不序列化null值
public class RAGFlowKnowledgeReq {
    @JsonProperty("question")
    private String question;

    @JsonProperty("dataset_ids")
    private List<String> datasetIds;

    @JsonProperty("document_ids")
    private List<String> documentIds;

    @JsonProperty("page")
    private Integer page = 1;

    @JsonProperty("page_size")
    private Integer pageSize = 10;

    @JsonProperty("similarity_threshold")
    private Double similarityThreshold;

    @JsonProperty("vector_similarity_weight")
    private Double vectorSimilarityWeight = 0.5;

    @JsonProperty("top_k")
    private Integer topK;

    @JsonProperty("rerank_id")
    private String rerankId;

    @JsonProperty("keyword")
    private Boolean keyword = true;

    @JsonProperty("highlight")
    private Boolean highlight;
}