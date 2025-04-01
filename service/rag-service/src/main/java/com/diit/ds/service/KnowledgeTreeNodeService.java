package com.diit.ds.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.diit.ds.domain.dto.KnowledgeTreeNodeDTO;
import com.diit.ds.domain.entity.KnowledgeTreeNode;
import com.diit.ds.domain.req.KnowledgeTreeNodeCreateReq;

import java.util.List;

/**
* @author test
* @description 针对表【knowledge_tree_node(知识中心 树节点表)】的数据库操作Service
* @createDate 2025-03-31 13:11:23
*/
public interface KnowledgeTreeNodeService extends IService<KnowledgeTreeNode> {
    KnowledgeTreeNode createNode(KnowledgeTreeNodeCreateReq knowledgeTreeNode);

    KnowledgeTreeNode updateNode(KnowledgeTreeNode knowledgeTreeNode);

    String deleteNode(String id);

    List<String> deleteNode(List<String> ids);

    KnowledgeTreeNode getNode(String id);

    List<KnowledgeTreeNode> listNode();

    List<String> getIdsByPid();

    List<String> getIdsByPid(String pid);

    KnowledgeTreeNodeDTO getTreeNodeDTO();
}
