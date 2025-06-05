package com.diit.ds.rag.domain.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.util.List;

/**
 * 知识查询结果
 */
@Data
public class KnowledgeSearchResultDTO {
    /**
     * 工作流运行ID
     */
    @JsonProperty("work_flow_run_id")
    private String workFlowRunId;

    /**
     * 文件引用切片列表
     */
    @JsonProperty("work_flow_chunks")
    private List<WorkflowChunkDTO> workflowChunks;

    /**
     * 文件引用文件列表
     */
    @JsonProperty("work_flow_doc_agg")
    private List<WorkflowDocAggDTO> workflowChunksFiles;
}