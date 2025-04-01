package com.diit.ds.controller;

import com.diit.ds.domain.dto.KnowledgeTreeNodeDTO;
import com.diit.ds.domain.entity.KnowledgeTreeNode;
import com.diit.ds.domain.req.KnowledgeTreeNodeCreateReq;
import com.diit.ds.domain.req.KnowledgeTreeNodeUpdateReq;
import com.diit.ds.service.KnowledgeTreeNodeService;
import com.diit.ds.structmapper.KnowledgeTreeNodeSM;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "知识库管理", description = "知识库树结构管理相关接口")
@RestController
@RequestMapping("/api/v1/knowledge")
@RequiredArgsConstructor
public class KnowledgeController {

    private final KnowledgeTreeNodeService knowledgeTreeNodeService;

    @Operation(summary = "创建知识树节点", description = "创建一个新的知识树节点")
    @PostMapping("/tree/node")
    public ResponseEntity<?> createTreeNode(@RequestBody KnowledgeTreeNodeCreateReq createReq) {
        KnowledgeTreeNode node = knowledgeTreeNodeService.createNode(createReq);
        return ResponseEntity.status(HttpStatus.CREATED).body(node);
    }

    @Operation(summary = "更新知识树节点", description = "更新指定ID的知识树节点")
    @PutMapping("/tree/node/{id}")
    public ResponseEntity<?> updateTreeNode(
            @Parameter(description = "知识树节点ID") @PathVariable String id,
            @RequestBody KnowledgeTreeNodeUpdateReq updateReq) {
        KnowledgeTreeNode knowledgeTreeNode = KnowledgeTreeNodeSM.INSTANCE.updateDTO2Entity(updateReq);
        knowledgeTreeNode.setId(id);
        KnowledgeTreeNode node = knowledgeTreeNodeService.updateNode(knowledgeTreeNode);
        return ResponseEntity.status(HttpStatus.OK).body(node);
    }

    @Operation(summary = "删除知识树节点", description = "删除指定ID的知识树节点")
    @DeleteMapping("/tree/node/{id}")
    public ResponseEntity<?> deleteTreeNode(@Parameter(description = "知识树节点ID") @PathVariable String id) {
        knowledgeTreeNodeService.deleteNode(id);
        return ResponseEntity.status(HttpStatus.OK).body(id);
    }

//    @Operation(summary = "批量删除知识树节点", description = "批量删除指定ID列表的知识树节点")
//    @DeleteMapping("/tree/nodes")
//    public ResponseEntity<?> deleteTreeNodes(@RequestBody List<String> ids) {
//        knowledgeTreeNodeService.deleteNode(ids);
//        return ResponseEntity.status(HttpStatus.OK).body(ids);
//    }

//    @Operation(summary = "获取知识树节点", description = "获取指定ID的知识树节点详情")
//    @GetMapping("/tree/node/{id}")
//    public ResponseEntity<?> getTreeNode(@Parameter(description = "知识树节点ID") @PathVariable String id) {
//        KnowledgeTreeNode node = knowledgeTreeNodeService.getNode(id);
//        if (node != null) {
//            return ResponseEntity.ok(node);
//        } else {
//            return ResponseEntity.notFound().build();
//        }
//    }

//    @Operation(summary = "获取所有知识树节点", description = "获取所有知识树节点列表")
//    @GetMapping("/tree/nodes")
//    public ResponseEntity<List<KnowledgeTreeNode>> listTreeNodes() {
//        List<KnowledgeTreeNode> nodes = knowledgeTreeNodeService.listNode();
//        return ResponseEntity.ok(nodes);
//    }

//    @Operation(summary = "获取指定父节点的子节点ID列表", description = "获取指定父节点ID下的所有子节点ID列表")
//    @GetMapping("/tree/node/children")
//    public ResponseEntity<List<String>> listChildNodeIds(
//            @Parameter(description = "父节点ID，不传则获取顶级节点") @RequestParam(required = false) String pid) {
//        List<String> ids;
//        if (pid != null) {
//            // 调用重载的方法获取指定父节点的子节点
//            ids = ((com.diit.ds.service.impl.KnowledgeTreeNodeServiceImpl) knowledgeTreeNodeService).getIdsByPid(pid);
//        } else {
//            // 获取顶级节点
//            ids = knowledgeTreeNodeService.getIdsByPid();
//        }
//        return ResponseEntity.ok(ids);
//    }

    @Operation(summary = "获取知识树结构", description = "获取完整的知识树结构")
    @GetMapping("/tree")
    public ResponseEntity<KnowledgeTreeNodeDTO> getKnowledgeTree() {
        KnowledgeTreeNodeDTO tree = knowledgeTreeNodeService.getTreeNodeDTO();
        if (tree != null) {
            return ResponseEntity.ok(tree);
        } else {
            return ResponseEntity.noContent().build();
        }
    }
}
