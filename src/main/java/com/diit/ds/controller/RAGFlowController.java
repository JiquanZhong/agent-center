package com.diit.ds.controller;


import com.diit.ds.domain.req.RAGFlowKnowledgeReq;
import com.diit.ds.domain.resp.RAGFlowKnowledgeResp;
import com.diit.ds.service.RAGFlowAPIService;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

@Slf4j
@Tag(name = "RAGFlow知识库的Controller")
@RestController
@RequestMapping("/v1/ragflow")
@RequiredArgsConstructor
public class RAGFlowController {
    private final RAGFlowAPIService ragFlowAPIService;


    @GetMapping("/retrieval")
    public Map retrieval(String query, String datasetId) {
        RAGFlowKnowledgeReq ragFlowReq = new RAGFlowKnowledgeReq();
        ragFlowReq.setQuestion(query);
//        ragFlowReq.setDatasetIds(Arrays.asList("3bd65786ff2911efa9fe0242ac120006"));
        ragFlowReq.setDatasetIds(Arrays.asList(datasetId));
        ragFlowReq.setSimilarityThreshold(0.4);
        ragFlowReq.setTopK(3);
        ragFlowReq.setPage(1);
        ragFlowReq.setPageSize(1);

        RAGFlowKnowledgeResp resp = ragFlowAPIService.retrieval(ragFlowReq);

        return new HashMap<String, Object>() {{
            put("chunks", resp.getData().getChunks());
            put("doc_aggs", resp.getData().getDocAggs());
        }};
    }
}
