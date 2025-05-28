package com.diit.ds.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.diit.ds.domain.dto.KnowledgeTreeNodeDTO;
import com.diit.ds.domain.dto.KnowledgeTreeStatisticDTO;
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

    /**
     * 删除节点
     * @param id 节点ID
     * @param callRAGFlowAPI 是否调用RAGFlow API删除数据集
     * @return 删除的节点ID
     */
    String deleteNode(String id, boolean callRAGFlowAPI);

    List<String> deleteNode(List<String> ids);

    /**
     * 批量删除节点
     * @param ids 节点ID列表
     * @param callRAGFlowAPI 是否调用RAGFlow API删除数据集
     * @return 删除的节点ID列表
     */
    List<String> deleteNode(List<String> ids, boolean callRAGFlowAPI);

    KnowledgeTreeNode getNode(String id);

    List<KnowledgeTreeNode> listNode();

    List<String> getIdsByPid();

    List<String> getIdsByPid(String pid);

    List<KnowledgeTreeNode> getNodesByPid(String pid);

    List<String> getKbIdsByPid(String pid);

    KnowledgeTreeNodeDTO getTreeNodeDTO();
    
    /**
     * 更新所有节点的文档数量
     */
    void updateAllNodesStatistic();
    
    /**
     * 计算并更新指定节点的文档数量
     * @param nodeId 节点ID
     * @return 更新后的文档数量
     */
    void updateNode(String nodeId);
    
    /**
     * 更新节点及其所有父节点的文档数量
     * @param nodeId 节点ID
     * @param documentNumDelta 文档数量的变化值，正数表示增加，负数表示减少
     */
    void updateNodeAndParentsDocumentNum(String nodeId, Integer documentNumDelta);

    /**
     * 获取知识树节点的统计信息
     * @param pid 节点ID
     * @return 知识树节点统计信息
     */
    KnowledgeTreeStatisticDTO getTreeNodeStatisticDTO(String pid);

}
