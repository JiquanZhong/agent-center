package com.diit.ds.service;

import com.diit.ds.domain.dto.KnowledgeSearchResultDTO;
import com.diit.ds.domain.resp.FilePreviewResp;
import com.diit.ds.exception.FileNotFoundException;

/**
* @author test
* @createDate 2025-03-31 13:11:23
*/
public interface KnowledgeFilePreviewService {

    /**
     * 获取文件预览URL
     *
     * @param documentId 文档ID
     * @return 文件预览响应，包含预览URL
     * @throws FileNotFoundException 文件不存在时抛出异常
     */
    FilePreviewResp previewFile(String documentId) throws FileNotFoundException;

    /**
     * 根据work_flow_run_id获取知识查询的相关信息
     */
    KnowledgeSearchResultDTO getKnowledgeQueryInfo(String workFlowRunId);
}
