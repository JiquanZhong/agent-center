package com.diit.ds.controller;

import com.diit.ds.domain.req.*;
import com.diit.ds.domain.resp.*;
import com.diit.ds.exception.FileNotFoundException;
import com.diit.ds.service.KnowledgeFileService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;

@Tag(name = "知识库文件管理", description = "知识库文件管理相关接口")
@RestController
@RequestMapping("/api/v1/knowledge/file")
@RequiredArgsConstructor
public class KnowledgeFileController {
    private final KnowledgeFileService knowledgeFileService;

    @Operation(summary = "上传文件到知识中心节点", description = "上传文件到指定的知识库节点")
    @PostMapping(value = "/upload/{treeNodeId}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<RAGFlowFileUploadResp> uploadFiles(
            @Parameter(description = "知识库节点ID") @PathVariable String treeNodeId,
            @Parameter(description = "上传文件") @RequestParam("files") MultipartFile[] files) {
        RAGFlowFileUploadResp result = knowledgeFileService.uploadFiles(treeNodeId, files);
        return ResponseEntity.ok(result);
    }

    @Operation(summary = "下载文件", description = "根据文档ID下载文件")
    @GetMapping("/download/{documentId}")
    public void downloadFile(
            @Parameter(description = "文档ID") @PathVariable String documentId,
            HttpServletResponse response) {
        try {
            String fileName = knowledgeFileService.downloadFile(documentId, response.getOutputStream());
            response.setContentType("application/octet-stream");
            response.setHeader("Content-Disposition", "attachment; filename=\"" + fileName + "\"");
        } catch (FileNotFoundException e) {
            response.setStatus(HttpStatus.NOT_FOUND.value());
        } catch (IOException e) {
            response.setStatus(HttpStatus.INTERNAL_SERVER_ERROR.value());
        }
    }

    @Operation(summary = "查看文件列表", description = "获取知识库节点下的文件列表")
    @GetMapping("/list/{treeNodeId}")
    public ResponseEntity<RAGFlowFileListResp> listFiles(
            @Parameter(description = "知识库节点ID") @PathVariable String treeNodeId,
            @Parameter(description = "当前页码") @RequestParam(required = false) Integer page,
            @Parameter(description = "每页数量") @RequestParam(required = false) Integer pageSize,
            @Parameter(description = "排序字段") @RequestParam(required = false) String orderby,
            @Parameter(description = "是否降序") @RequestParam(required = false) Boolean desc,
            @Parameter(description = "关键词") @RequestParam(required = false) String keywords,
            @Parameter(description = "文档ID") @RequestParam(required = false) String id,
            @Parameter(description = "文档名称") @RequestParam(required = false) String name) {
        RAGFlowFileListReq req = RAGFlowFileListReq.builder()
                .page(page)
                .pageSize(pageSize)
                .orderby(orderby)
                .desc(desc)
                .keywords(keywords)
                .id(id)
                .name(name)
                .build();
        RAGFlowFileListResp result = knowledgeFileService.listFiles(treeNodeId, req);
        return ResponseEntity.ok(result);
    }

    @Operation(summary = "删除文件", description = "删除知识库中的文件")
    @DeleteMapping("/delete")
    public ResponseEntity<RAGFlowFileDeleteResp> deleteFiles(
            @Parameter(description = "删除文件请求") @RequestBody RAGFlowFileDeleteReq req) {
        RAGFlowFileDeleteResp result = knowledgeFileService.deleteFiles(req);
        return ResponseEntity.ok(result);
    }

    @Operation(summary = "开始解析文件任务", description = "开始解析文件任务")
    @PostMapping("/parse/start")
    public ResponseEntity<RAGFlowFileParseResp> startParseTask(
            @Parameter(description = "解析文件请求") @RequestBody RAGFlowFileParseReq req) {
        RAGFlowFileParseResp result = knowledgeFileService.startParseTask(req);
        return ResponseEntity.ok(result);
    }

    @Operation(summary = "停止解析文件任务", description = "停止解析文件任务")
    @PostMapping("/parse/stop")
    public ResponseEntity<RAGFlowFileParseResp> stopParseTask(
            @Parameter(description = "停止解析文件请求") @RequestBody RAGFlowFileParseReq req) {
        RAGFlowFileParseResp result = knowledgeFileService.stopParseTask(req);
        return ResponseEntity.ok(result);
    }

    @Operation(summary = "查看文档的切片列表", description = "获取指定文档的切片列表")
    @GetMapping("/chunks/{documentId}")
    public ResponseEntity<RagFlowChunkListResp> listChunks(
            @Parameter(description = "文档ID") @PathVariable String documentId,
            @Parameter(description = "当前页码") @RequestParam(required = false) Integer page,
            @Parameter(description = "每页数量") @RequestParam(required = false) Integer pageSize,
            @Parameter(description = "关键词") @RequestParam(required = false) String keywords,
            @Parameter(description = "切片ID") @RequestParam(required = false) String id) {
        RagFlowChunkListReq req = RagFlowChunkListReq.builder()
                .page(page)
                .pageSize(pageSize)
                .keywords(keywords)
                .id(id)
                .build();
        RagFlowChunkListResp result = knowledgeFileService.listChunks(documentId, req);
        return ResponseEntity.ok(result);
    }

//     @Operation(summary = "更新文档切片内容", description = "更新指定文档切片的内容")
//     @PutMapping("/chunks/{chunkId}")
//     public ResponseEntity<RagFlowChunkUpdateResp> updateChunk(
//             @Parameter(description = "切片ID") @PathVariable String chunkId,
//             @Parameter(description = "更新请求") @RequestBody RagFlowChunkUpdateReq req) {
//         RagFlowChunkUpdateResp result = knowledgeFileService.updateChunk(chunkId, req);
//         return ResponseEntity.ok(result);
//     }
}
