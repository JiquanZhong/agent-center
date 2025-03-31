package com.diit.ds.domain.resp;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@Data
public class RAGFlowKnowledgeResp {
    @JsonProperty("code")
    private int code;

    @JsonProperty("data")
    private RespData data;

    @JsonProperty("message")
    private String message;

    @Data
    public static class RespData {
        @JsonProperty("chunks")
        private List<Chunk> chunks;

        @JsonProperty("doc_aggs")
        private List<DocAgg> docAggs;

        @JsonProperty("total")
        private int total;

        @Data
        public static class Chunk {
            @JsonProperty("content")
            private String content;

            @JsonProperty("content_ltks")
            private String contentLtks;

            @JsonProperty("dataset_id")
            private Object datasetId;

            public String getDatasetIdAsString() {
                if (datasetId == null) {
                    return null;
                }
                if (datasetId instanceof String) {
                    return (String) datasetId;
                }
                if (datasetId instanceof List && !((List<?>) datasetId).isEmpty()) {
                    return ((List<?>) datasetId).get(0).toString();
                }
                return datasetId.toString();
            }

            public List<String> getDatasetIdAsList() {
                if (datasetId == null) {
                    return Collections.emptyList();
                }
                if (datasetId instanceof String) {
                    return Collections.singletonList((String) datasetId);
                }
                if (datasetId instanceof List) {
                    List<String> result = new ArrayList<>();
                    for (Object item : (List<?>) datasetId) {
                        result.add(item.toString());
                    }
                    return result;
                }
                return Collections.singletonList(datasetId.toString());
            }

            @JsonProperty("document_id")
            private String documentId;

            @JsonProperty("document_keyword")
            private String documentKeyword;

            @JsonProperty("highlight")
            private String highlight;

            @JsonProperty("id")
            private String id;

            @JsonProperty("image_id")
            private String imageId;

            @JsonProperty("important_keywords")
            private List<String> importantKeywords;

            @JsonProperty("kb_id")
            private String kbId;

            @JsonProperty("positions")
            private List<List<Integer>> positions;

            @JsonProperty("similarity")
            private double similarity;

            @JsonProperty("term_similarity")
            private double termSimilarity;

            @JsonProperty("vector_similarity")
            private double vectorSimilarity;
        }

        @Data
        public static class DocAgg {
            @JsonProperty("count")
            private int count;

            @JsonProperty("doc_id")
            private String docId;

            @JsonProperty("doc_name")
            private String docName;
        }
    }
}