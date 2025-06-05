package com.diit.ds.web;

import com.diit.ds.rag.domain.dto.KnowledgeSearchResultDTO;
import com.diit.ds.rag.domain.resp.FilePreviewResp;
import com.diit.ds.common.exception.FileNotFoundException;
import com.diit.ds.rag.service.KnowledgeFilePreviewService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "知识库文件浏览", description = "知识库文件浏览相关接口")
@RestController
@RequestMapping("/api/preview")
@RequiredArgsConstructor
@Slf4j
public class KnowledgeFilePreviewController {

    private final KnowledgeFilePreviewService knowledgeFilePreviewService;

    /**
     * 获取文件预览URL
     *
     * @param documentId 文档ID
     * @return 文件预览响应，包含预览URL
     */
    @Operation(summary = "获取文件预览URL", description = "根据文档ID获取文件预览URL")
    @GetMapping("/{documentId}")
    public ResponseEntity<FilePreviewResp> previewFile(
            @Parameter(description = "文档ID", required = true)
            @PathVariable String documentId) {
        try {
            FilePreviewResp response = knowledgeFilePreviewService.previewFile(documentId);
            return ResponseEntity.ok(response);
        } catch (FileNotFoundException e) {
            return ResponseEntity.notFound().build();
        } catch (Exception e) {
            return ResponseEntity.badRequest().build();
        }
    }

    /**
     * 获取知识查询信息
     *
     * @param workFlowRunId 工作流运行ID
     * @return 知识查询结果，包含文档片段和文件信息
     */
    @Operation(summary = "获取知识查询信息", description = "根据工作流运行ID获取知识查询相关信息")
    @GetMapping("/knowledge/{workFlowRunId}")
    public ResponseEntity<?> getKnowledgeQueryInfo(
            @Parameter(description = "工作流运行ID", required = true)
            @PathVariable String workFlowRunId) {
        try {
            KnowledgeSearchResultDTO resultDTO =
                knowledgeFilePreviewService.getKnowledgeQueryInfo(workFlowRunId);
            return ResponseEntity.ok(resultDTO);
        } catch (Exception e) {
            log.error("获取知识查询信息失败: {}", e.getMessage(), e);
            return ResponseEntity.badRequest().body("获取知识查询信息失败: " + e.getMessage());
        }
    }
    
}
