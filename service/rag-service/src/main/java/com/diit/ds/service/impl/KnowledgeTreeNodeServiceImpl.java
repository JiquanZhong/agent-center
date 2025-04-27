package com.diit.ds.service.impl;

import com.baomidou.dynamic.datasource.annotation.DS;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.diit.ds.domain.dto.KnowledgeTreeNodeDTO;
import com.diit.ds.domain.entity.KnowledgeTreeNode;
import com.diit.ds.domain.req.KnowledgeTreeNodeCreateReq;
import com.diit.ds.domain.req.RAGFlowDatasetCreateReq;
import com.diit.ds.domain.req.RAGFlowDatasetDeleteReq;
import com.diit.ds.domain.req.RAGFlowDatasetUpdateReq;
import com.diit.ds.domain.resp.RAGFlowDatasetCreateResp;
import com.diit.ds.domain.resp.RAGFlowDatasetDeleteResp;
import com.diit.ds.domain.resp.RAGFlowDatasetUpdateResp;
import com.diit.ds.service.KnowledgeTreeNodeService;
import com.diit.ds.mapper.KnowledgeTreeNodeMapper;
import com.diit.ds.service.RAGFlowDBAPIService;
import com.diit.ds.structmapper.KnowledgeTreeNodeSM;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;
import java.util.Map;
import java.util.HashMap;

import com.diit.ds.service.DocumentService;

/**
* @author test
* @description 针对表【knowledge_tree_node(知识中心 树节点表)】的数据库操作Service实现
* @createDate 2025-03-31 13:11:23
*/
@Slf4j
@Service
@RequiredArgsConstructor
@DS("primary")
public class KnowledgeTreeNodeServiceImpl extends ServiceImpl<KnowledgeTreeNodeMapper, KnowledgeTreeNode> implements KnowledgeTreeNodeService {

    private final RAGFlowDBAPIService ragFlowDBAPIService;
    private final DocumentService documentService;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public KnowledgeTreeNode createNode(KnowledgeTreeNodeCreateReq createReq) {
        KnowledgeTreeNode knowledgeTreeNode = KnowledgeTreeNodeSM.INSTANCE.createDTO2Entity(createReq);
        
        // 获取节点类型
        String nodeType = createReq.getType();
        log.info("创建知识树节点，节点名称: {}, 节点类型: {}", knowledgeTreeNode.getName(), nodeType);
        
        // 创建RAGFlow数据集
        RAGFlowDatasetCreateResp resp;
        
        // 根据节点类型创建不同类型的数据集
        if (nodeType != null) {
            switch (nodeType.toLowerCase()) {
                case "general":
                case "通用":
                    resp = ragFlowDBAPIService.createGeneralDataset(
                            knowledgeTreeNode.getName(),
                            knowledgeTreeNode.getDescription(),
                            "team");
                    break;
                case "laws":
                case "法律":
                    resp = ragFlowDBAPIService.createLawsDataset(
                            knowledgeTreeNode.getName(),
                            knowledgeTreeNode.getDescription(),
                            "team");
                    break;
                case "paper":
                case "论文":
                    resp = ragFlowDBAPIService.createPaperDataset(
                            knowledgeTreeNode.getName(),
                            knowledgeTreeNode.getDescription(),
                            "team");
                    break;
                case "book":
                case "书籍":
                    resp = ragFlowDBAPIService.createBookDataset(
                            knowledgeTreeNode.getName(),
                            knowledgeTreeNode.getDescription(),
                            "team");
                    break;
                case "qa":
                case "问答":
                case "问答对":
                    resp = ragFlowDBAPIService.createQADataset(
                            knowledgeTreeNode.getName(),
                            knowledgeTreeNode.getDescription(),
                            "team");
                    break;
                default:
                    // 默认创建通用类型数据集
                    log.info("未知节点类型: {}，将创建通用类型数据集", nodeType);
                    resp = ragFlowDBAPIService.createGeneralDataset(
                            knowledgeTreeNode.getName(),
                            knowledgeTreeNode.getDescription(),
                            "team");
                    break;
            }
        } else {
            // 如果未指定类型，默认创建通用类型数据集
            log.info("未指定节点类型，将创建通用类型数据集");
            resp = ragFlowDBAPIService.createGeneralDataset(
                    knowledgeTreeNode.getName(),
                    knowledgeTreeNode.getDescription(),
                    "team");
        }

        // 检查API调用结果
        if (resp != null && resp.getCode() == 0 && resp.getData() != null) {
            // 查询父节点信息
            if (knowledgeTreeNode.getPid() == null || knowledgeTreeNode.getPid().isEmpty() || "0".equals(knowledgeTreeNode.getPid())) {
                knowledgeTreeNode.setPid("0");
                knowledgeTreeNode.setLevel(1);
            } else {
                KnowledgeTreeNode pNode = lambdaQuery()
                        .eq(KnowledgeTreeNode::getId, knowledgeTreeNode.getPid())
                        .one();
                if (pNode == null) {
                    log.error("创建失败：找不到父节点，父节点ID: {}", knowledgeTreeNode.getPid());
                    throw new RuntimeException("找不到父节点");
                }
                knowledgeTreeNode.setLevel(pNode.getLevel() + 1);
            }

            // 设置数据库记录的相关字段
            knowledgeTreeNode.setKdbId(resp.getData().getId());
            knowledgeTreeNode.setRagflowName(resp.getData().getName());
            knowledgeTreeNode.setEmbeddingsModel(resp.getData().getEmbeddingModel());
            knowledgeTreeNode.setType(nodeType); // 设置节点类型

            // 设置创建和更新时间
            Date now = new Date();
            knowledgeTreeNode.setCreateTime(now);
            knowledgeTreeNode.setUpdateTime(now);
            
            // 保存到数据库
            if(baseMapper.insert(knowledgeTreeNode) != 1) {
                log.error("知识树节点创建失败，ID: {}, RAGFlow数据集ID: {}", knowledgeTreeNode.getId(), knowledgeTreeNode.getKdbId());
                throw new RuntimeException("知识树节点创建失败");
            }
            // log.info("知识树节点创建成功，ID: {}, RAGFlow数据集ID: {}", knowledgeTreeNode.getId(), knowledgeTreeNode.getKdbId());
            log.info("知识树节点创建成功，ID: {}, RAGFlow数据集ID: {}, 类型: {}", knowledgeTreeNode.getId(), knowledgeTreeNode.getKdbId(), nodeType);
            return knowledgeTreeNode;
        } else {
            log.error("创建RAGFlow数据集失败: {}", resp != null ? resp.getMessage() : "响应为空");
            // 删除RAGFlow数据集
            if (resp != null && resp.getData() != null) {
                RAGFlowDatasetDeleteReq deleteReq = new RAGFlowDatasetDeleteReq();
                deleteReq.setIds(Arrays.asList(resp.getData().getId()));
                ragFlowDBAPIService.deleteDatasets(deleteReq);
            }
            throw new RuntimeException("创建RAGFlow数据集失败: " + (resp != null ? resp.getMessage() : "响应为空"));
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public KnowledgeTreeNode updateNode(KnowledgeTreeNode knowledgeTreeNode) {
        // 获取原始记录以获取kdb_id
        KnowledgeTreeNode existingNode = getById(knowledgeTreeNode.getId());
        if (existingNode == null || existingNode.getKdbId() == null) {
            log.error("更新失败：找不到原始知识树节点或其RAGFlow数据集ID为空，节点ID: {}", knowledgeTreeNode.getId());
            throw new RuntimeException("找不到原始知识树节点或其RAGFlow数据集ID为空");
        }
        
        // 创建RAGFlow数据集更新请求
        RAGFlowDatasetUpdateReq req = new RAGFlowDatasetUpdateReq();
        req.setName(knowledgeTreeNode.getRagflowName() != null ? 
                knowledgeTreeNode.getRagflowName() : knowledgeTreeNode.getName());
        
        // 调用RAGFlow API更新数据集
        RAGFlowDatasetUpdateResp resp = ragFlowDBAPIService.updateDataset(existingNode.getKdbId(), req);
        
        // 检查API调用结果
        if (resp != null && resp.getCode() == 0) {
            // 设置更新时间
            knowledgeTreeNode.setUpdateTime(new Date());
            
            // 更新数据库
            updateById(knowledgeTreeNode);
            log.info("知识树节点更新成功，ID: {}, RAGFlow数据集ID: {}", knowledgeTreeNode.getId(), existingNode.getKdbId());
            return knowledgeTreeNode;
        } else {
            log.error("更新RAGFlow数据集失败: {}", resp != null ? resp.getMessage() : "响应为空");
            throw new RuntimeException("更新RAGFlow数据集失败: " + (resp != null ? resp.getMessage() : "响应为空"));
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public String deleteNode(String id) {
        // 获取节点信息以获取kdb_id
        KnowledgeTreeNode node = getById(id);
        if (node == null || node.getKdbId() == null) {
            log.error("删除失败：找不到知识树节点或其RAGFlow数据集ID为空，节点ID: {}", id);
            throw new RuntimeException("找不到知识树节点或其RAGFlow数据集ID为空");
        }
        
        // 获取当前节点的父节点ID和文档数量
        String parentId = node.getPid();
        Integer documentNum = node.getDocumentNum() != null ? node.getDocumentNum() : 0;
        
        // 获取当前节点及其所有子孙节点的ID
        List<String> allNodeIds = getIdsByPid(id);
        
        // 获取所有节点的kdbId
        List<KnowledgeTreeNode> allNodes = listByIds(allNodeIds);
        List<String> allKdbIds = allNodes.stream()
                .filter(n -> n.getKdbId() != null)
                .map(KnowledgeTreeNode::getKdbId)
                .collect(Collectors.toList());
        
        if (allKdbIds.isEmpty()) {
            log.error("删除失败：所有知识树节点的RAGFlow数据集ID均为空，节点ID列表: {}", allNodeIds);
            throw new RuntimeException("所有知识树节点的RAGFlow数据集ID均为空");
        }
        
        // 创建RAGFlow数据集删除请求
        RAGFlowDatasetDeleteReq req = new RAGFlowDatasetDeleteReq();
        req.setIds(allKdbIds);
        
        // 调用RAGFlow API删除数据集
        RAGFlowDatasetDeleteResp resp = ragFlowDBAPIService.deleteDatasets(req);
        
        // 检查API调用结果
        if (resp != null && resp.getCode() == 0) {
            // 从数据库中删除所有记录
            removeByIds(allNodeIds);
            log.info("知识树节点及其子节点删除成功，根节点ID: {}, 删除节点总数: {}, RAGFlow数据集ID列表: {}", 
                    id, allNodeIds.size(), allKdbIds);
            
            // 更新父节点的文档数量
            if (parentId != null && !parentId.isEmpty() && !parentId.equals("0") && documentNum > 0) {
                try {
                    // 减少父节点及其父节点的文档数量
                    updateNodeAndParentsDocumentNum(parentId, -documentNum);
                    log.info("已更新父节点[{}]及其父节点的文档数量，减少文档数: {}", parentId, documentNum);
                } catch (Exception e) {
                    log.error("更新父节点文档数量失败: {}", e.getMessage(), e);
                }
            }
            
            return node.getId();
        } else {
            log.error("删除RAGFlow数据集失败: {}", resp != null ? resp.getMessage() : "响应为空");
            throw new RuntimeException("删除RAGFlow数据集失败: " + (resp != null ? resp.getMessage() : "响应为空"));
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public List<String> deleteNode(List<String> ids) {
        if (ids == null || ids.isEmpty()) {
            log.warn("删除操作传入的ID列表为空");
            return new ArrayList<>();
        }
        
        // 批量获取节点信息以获取kdb_id列表
        List<KnowledgeTreeNode> nodes = listByIds(ids);
        if (nodes.isEmpty()) {
            log.error("删除失败：找不到指定ID的知识树节点，节点ID列表: {}", ids);
            throw new RuntimeException("找不到指定ID的知识树节点");
        }
        
        // 记录每个父节点需要减少的文档数量
        Map<String, Integer> parentDocumentNumMap = new HashMap<>();
        for (KnowledgeTreeNode node : nodes) {
            String parentId = node.getPid();
            if (parentId != null && !parentId.isEmpty() && !parentId.equals("0")) {
                Integer documentNum = node.getDocumentNum() != null ? node.getDocumentNum() : 0;
                if (documentNum > 0) {
                    parentDocumentNumMap.put(parentId, 
                            parentDocumentNumMap.getOrDefault(parentId, 0) + documentNum);
                }
            }
        }
        
        // 提取所有有效的kdb_id
        List<String> kdbIds = nodes.stream()
                .filter(node -> node.getKdbId() != null)
                .map(KnowledgeTreeNode::getKdbId)
                .collect(Collectors.toList());
        
        if (kdbIds.isEmpty()) {
            log.error("删除失败：所有知识树节点的RAGFlow数据集ID均为空，节点ID列表: {}", ids);
            throw new RuntimeException("所有知识树节点的RAGFlow数据集ID均为空");
        }
        
        // 创建RAGFlow数据集删除请求
        RAGFlowDatasetDeleteReq req = new RAGFlowDatasetDeleteReq();
        req.setIds(kdbIds);
        
        // 调用RAGFlow API删除数据集
        RAGFlowDatasetDeleteResp resp = ragFlowDBAPIService.deleteDatasets(req);
        
        // 检查API调用结果
        if (resp != null && resp.getCode() == 0) {
            // 从数据库中批量删除记录
            removeByIds(ids);
            log.info("批量删除知识树节点成功，节点ID列表: {}, RAGFlow数据集ID列表: {}", ids, kdbIds);
            
            // 更新所有受影响的父节点的文档数量
            for (Map.Entry<String, Integer> entry : parentDocumentNumMap.entrySet()) {
                String parentId = entry.getKey();
                Integer documentNum = entry.getValue();
                
                try {
                    // 减少父节点及其父节点的文档数量
                    updateNodeAndParentsDocumentNum(parentId, -documentNum);
                    log.info("已更新父节点[{}]及其父节点的文档数量，减少文档数: {}", parentId, documentNum);
                } catch (Exception e) {
                    log.error("更新父节点[{}]文档数量失败: {}", parentId, e.getMessage(), e);
                }
            }
            
            return ids;
        } else {
            log.error("批量删除RAGFlow数据集失败: {}", resp != null ? resp.getMessage() : "响应为空");
            throw new RuntimeException("批量删除RAGFlow数据集失败: " + (resp != null ? resp.getMessage() : "响应为空"));
        }
    }

    @Override
    public KnowledgeTreeNode getNode(String id) {
        return getById(id);
    }

    @Override
    public List<KnowledgeTreeNode> listNode() {
        return lambdaQuery().list();
    }

    @Override
    public List<String> getIdsByPid() {
        return getIdsByPid("0");
    }

    /**
     * 获取指定父节点下的所有子节点ID（包括所有层级的子孙节点和自身ID）
     * @param pid 父节点ID，如果为null则获取顶级节点
     * @return 子节点ID列表（包含所有层级）
     */
    @Override
    public List<String> getIdsByPid(String pid) {
        List<String> result = new ArrayList<>();
        
        // 如果pid不为空，先添加自身节点id
        if (pid != null && !pid.equals("0")) {
            result.add(pid);
        }
        
        // 递归获取所有子孙节点ID
        collectChildrenIds(pid, result);
        
        return result;
    }

    @Override
    public List<KnowledgeTreeNode> getNodesByPid(String pid) {
        List<String> ids = getIdsByPid(pid);
        return listByIds(ids);
    }

    @Override
    public List<String> getKbIdsByPid(String pid) {
        return getNodesByPid(pid).stream()
                .map(KnowledgeTreeNode::getKdbId)
                .filter(kdbId -> kdbId != null && !kdbId.isEmpty())
                .collect(Collectors.toList());
    }

    /**
     * 递归收集某节点下的所有子孙节点ID
     * @param parentId 父节点ID
     * @param idList 用于存储结果的列表
     */
    private void collectChildrenIds(String parentId, List<String> idList) {
        // 获取直接子节点
        List<KnowledgeTreeNode> directChildren = lambdaQuery()
                .eq(parentId != null, KnowledgeTreeNode::getPid, parentId)
                .isNull(parentId == null, KnowledgeTreeNode::getPid)
                .list();
        
        // 没有子节点则直接返回
        if (directChildren.isEmpty()) {
            return;
        }
        
        // 遍历每个直接子节点
        for (KnowledgeTreeNode child : directChildren) {
            // 添加子节点ID
            idList.add(child.getId());
            // 递归获取该子节点的所有子孙节点
            collectChildrenIds(child.getId(), idList);
        }
    }

    @Override
    public KnowledgeTreeNodeDTO getTreeNodeDTO() {
        // 获取所有节点
        List<KnowledgeTreeNode> allNodes = listNode();
        if (allNodes.isEmpty()) {
            return null;
        }
        
        // 构建虚拟根节点
        KnowledgeTreeNodeDTO root = new KnowledgeTreeNodeDTO();
        root.setId("0");
        root.setName("全部");
        root.setDescription("全部");
        root.setChildren(new ArrayList<>());
        
        // 构建节点映射，用于快速查找
        java.util.Map<String, KnowledgeTreeNodeDTO> nodeMap = new java.util.HashMap<>();
        nodeMap.put("0", root);
        
        // 第一步：将所有节点转换为DTO并放入映射
        for (KnowledgeTreeNode node : allNodes) {
            KnowledgeTreeNodeDTO dto = KnowledgeTreeNodeSM.INSTANCE.entity2DTO(node);
            dto.setChildren(new ArrayList<>());
            nodeMap.put(dto.getId(), dto);
        }
        
        // 第二步：构建树结构
        for (KnowledgeTreeNode node : allNodes) {
            String pid = node.getPid();
            // 如果父节点ID为空，则将其作为根节点的直接子节点
            if (pid == null || pid.isEmpty()) {
                pid = "0";
            }
            
            KnowledgeTreeNodeDTO parentDto = nodeMap.get(pid);
            KnowledgeTreeNodeDTO childDto = nodeMap.get(node.getId());
            
            if (parentDto != null && childDto != null) {
                parentDto.getChildren().add(childDto);
            } else {
                log.warn("构建树结构时发现无效的节点关系，节点ID: {}, 父节点ID: {}", node.getId(), pid);
            }
        }
        
        // 节点排序（如果sortOrder字段有值）
        sortTree(root);
        
        // 计算虚拟根节点的文档数量（所有节点文档数量的总和）
        int totalDocumentNum = 0;
        for (KnowledgeTreeNodeDTO childNode : root.getChildren()) {
            if (childNode.getDocumentNum() != null) {
                totalDocumentNum += childNode.getDocumentNum();
            }
        }
        root.setDocumentNum(totalDocumentNum);
        
        return root;
    }

    /**
     * 递归对树节点进行排序
     */
    private void sortTree(KnowledgeTreeNodeDTO node) {
        if (node.getChildren() != null && !node.getChildren().isEmpty()) {
            // 根据sortOrder字段排序，null值放在最后
            node.getChildren().sort((a, b) -> {
                if (a.getSortOrder() == null && b.getSortOrder() == null) {
                    return 0;
                } else if (a.getSortOrder() == null) {
                    return 1;
                } else if (b.getSortOrder() == null) {
                    return -1;
                } else {
                    return a.getSortOrder().compareTo(b.getSortOrder());
                }
            });
            
            // 递归排序子节点
            for (KnowledgeTreeNodeDTO child : node.getChildren()) {
                sortTree(child);
            }
        }
    }

    @Override
    public void updateAllNodesDocumentNum() {
        log.info("开始更新所有节点的文档数量");
        try {
            // 获取所有根节点
            List<KnowledgeTreeNode> rootNodes = lambdaQuery()
                    .eq(KnowledgeTreeNode::getPid, "0")
                    .list();
            
            // 递归更新每个根节点及其子节点的文档数量
            for (KnowledgeTreeNode rootNode : rootNodes) {
                updateNodeDocumentNum(rootNode.getId());
            }
            log.info("所有节点的文档数量更新完成");
        } catch (Exception e) {
            log.error("更新所有节点文档数量失败", e);
            throw new RuntimeException("更新所有节点文档数量失败: " + e.getMessage());
        }
    }
    
    @Override
    @Transactional(rollbackFor = Exception.class)
    public Integer updateNodeDocumentNum(String nodeId) {
        log.info("开始更新节点[{}]的文档数量", nodeId);
        try {
            // 获取节点信息
            KnowledgeTreeNode node = getById(nodeId);
            if (node == null) {
                log.error("找不到指定ID的知识树节点, 节点ID: {}", nodeId);
                return 0;
            }
            
            // 获取该节点自身的文档数量
            Integer ownDocumentNum = 0;
            if (node.getKdbId() != null && !node.getKdbId().isEmpty()) {
                ownDocumentNum = documentService.countDocumentsByKbId(node.getKdbId());
            }
            
            // 获取所有子节点
            List<KnowledgeTreeNode> childNodes = lambdaQuery()
                    .eq(KnowledgeTreeNode::getPid, nodeId)
                    .list();
            
            // 如果没有子节点，则文档数量就是自身的文档数量
            if (childNodes.isEmpty()) {
                // 更新节点的文档数量
                node.setDocumentNum(ownDocumentNum);
                updateById(node);
                log.info("节点[{}]没有子节点，文档数量设为自身文档数: {}", nodeId, ownDocumentNum);
                return ownDocumentNum;
            }
            
            // 递归计算所有子节点的文档数量
            Integer childrenDocumentNum = 0;
            for (KnowledgeTreeNode childNode : childNodes) {
                childrenDocumentNum += updateNodeDocumentNum(childNode.getId());
            }
            
            // 计算总文档数量（自身文档数量 + 子节点文档数量）
            Integer totalDocumentNum = ownDocumentNum + childrenDocumentNum;
            
            // 更新节点的文档数量
            node.setDocumentNum(totalDocumentNum);
            updateById(node);
            
            log.info("节点[{}]的文档数量更新完成，自身文档数: {}，子节点文档数: {}，总文档数: {}", 
                    nodeId, ownDocumentNum, childrenDocumentNum, totalDocumentNum);
            return totalDocumentNum;
        } catch (Exception e) {
            log.error("更新节点[{}]文档数量失败", nodeId, e);
            throw new RuntimeException("更新节点文档数量失败: " + e.getMessage());
        }
    }
    
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateNodeAndParentsDocumentNum(String nodeId, Integer documentNumDelta) {
        if (nodeId == null || nodeId.isEmpty() || nodeId.equals("0") || documentNumDelta == 0) {
            return;
        }
        
        log.info("开始更新节点[{}]及其所有父节点的文档数量，变化值: {}", nodeId, documentNumDelta);
        try {
            // 获取当前节点
            KnowledgeTreeNode node = getById(nodeId);
            if (node == null) {
                log.error("找不到指定ID的知识树节点, 节点ID: {}", nodeId);
                return;
            }
            
            // 更新当前节点的文档数量
            Integer currentDocumentNum = node.getDocumentNum() != null ? node.getDocumentNum() : 0;
            Integer newDocumentNum = currentDocumentNum + documentNumDelta;
            // 确保文档数量不会小于0
            newDocumentNum = Math.max(0, newDocumentNum);
            
            node.setDocumentNum(newDocumentNum);
            updateById(node);
            log.info("更新节点[{}]的文档数量: {} -> {}", nodeId, currentDocumentNum, newDocumentNum);
            
            // 如果有父节点，递归更新父节点
            String pid = node.getPid();
            if (pid != null && !pid.isEmpty() && !pid.equals("0")) {
                updateNodeAndParentsDocumentNum(pid, documentNumDelta);
            }
        } catch (Exception e) {
            log.error("更新节点[{}]及其父节点文档数量失败", nodeId, e);
            throw new RuntimeException("更新节点及其父节点文档数量失败: " + e.getMessage());
        }
    }
}




