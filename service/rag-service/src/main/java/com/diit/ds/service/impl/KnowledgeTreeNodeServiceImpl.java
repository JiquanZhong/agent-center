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
import com.diit.ds.service.RAGFlowAPIService;
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

    private final RAGFlowAPIService ragFlowAPIService;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public KnowledgeTreeNode createNode(KnowledgeTreeNodeCreateReq createReq) {
        KnowledgeTreeNode knowledgeTreeNode = KnowledgeTreeNodeSM.INSTANCE.createDTO2Entity(createReq);
        // 创建RAGFlow数据集请求
        RAGFlowDatasetCreateReq req = new RAGFlowDatasetCreateReq();
        req.setName(knowledgeTreeNode.getName());
        req.setDescription(knowledgeTreeNode.getDescription());
        
        // 调用RAGFlow API创建数据集
        RAGFlowDatasetCreateResp resp = ragFlowAPIService.createDataset(req);

        // 检查API调用结果
        if (resp != null && resp.getCode() == 0 && resp.getData() != null) {
            // 查询父节点信息
            if (knowledgeTreeNode.getPid() == null || knowledgeTreeNode.getPid().isEmpty()) {
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

            // 设置创建和更新时间
            Date now = new Date();
            knowledgeTreeNode.setCreateTime(now);
            knowledgeTreeNode.setUpdateTime(now);
            
            // 保存到数据库
            if(baseMapper.insert(knowledgeTreeNode) != 1) {
                log.error("知识树节点创建失败，ID: {}, RAGFlow数据集ID: {}", knowledgeTreeNode.getId(), knowledgeTreeNode.getKdbId());
                throw new RuntimeException("知识树节点创建失败");
            }
            log.info("知识树节点创建成功，ID: {}, RAGFlow数据集ID: {}", knowledgeTreeNode.getId(), knowledgeTreeNode.getKdbId());
            return knowledgeTreeNode;
        } else {
            log.error("创建RAGFlow数据集失败: {}", resp != null ? resp.getMessage() : "响应为空");
            // 删除RAGFlow数据集
            if (resp != null && resp.getData() != null) {
                RAGFlowDatasetDeleteReq deleteReq = new RAGFlowDatasetDeleteReq();
                deleteReq.setIds(Arrays.asList(resp.getData().getId()));
                ragFlowAPIService.deleteDatasets(deleteReq);
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
        RAGFlowDatasetUpdateResp resp = ragFlowAPIService.updateDataset(existingNode.getKdbId(), req);
        
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
        RAGFlowDatasetDeleteResp resp = ragFlowAPIService.deleteDatasets(req);
        
        // 检查API调用结果
        if (resp != null && resp.getCode() == 0) {
            // 从数据库中删除所有记录
            removeByIds(allNodeIds);
            log.info("知识树节点及其子节点删除成功，根节点ID: {}, 删除节点总数: {}, RAGFlow数据集ID列表: {}", 
                    id, allNodeIds.size(), allKdbIds);
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
        RAGFlowDatasetDeleteResp resp = ragFlowAPIService.deleteDatasets(req);
        
        // 检查API调用结果
        if (resp != null && resp.getCode() == 0) {
            // 从数据库中批量删除记录
            removeByIds(ids);
            log.info("批量删除知识树节点成功，节点ID列表: {}, RAGFlow数据集ID列表: {}", ids, kdbIds);
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
}




