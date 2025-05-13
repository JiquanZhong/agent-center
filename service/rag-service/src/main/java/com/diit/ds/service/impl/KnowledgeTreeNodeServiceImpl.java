package com.diit.ds.service.impl;

import com.baomidou.dynamic.datasource.annotation.DS;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.diit.ds.domain.dto.KnowledgeTreeNodeDTO;
import com.diit.ds.domain.dto.KnowledgeTreeStatisticDTO;
import com.diit.ds.domain.entity.KnowledgeTreeNode;
import com.diit.ds.domain.entity.Knowledgebase;
import com.diit.ds.domain.req.KnowledgeTreeNodeCreateReq;
import com.diit.ds.domain.req.RAGFlowDatasetDeleteReq;
import com.diit.ds.domain.req.RAGFlowDatasetUpdateReq;
import com.diit.ds.domain.resp.RAGFlowDatasetCreateResp;
import com.diit.ds.domain.resp.RAGFlowDatasetDeleteResp;
import com.diit.ds.domain.resp.RAGFlowDatasetUpdateResp;
import com.diit.ds.service.KnowledgeTreeNodeService;
import com.diit.ds.mapper.KnowledgeTreeNodeMapper;
import com.diit.ds.service.KnowledgebaseService;
import com.diit.ds.service.RAGFlowDBAPIService;
import com.diit.ds.structmapper.KnowledgeTreeNodeSM;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
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
import lombok.Data;

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
    private final KnowledgebaseService knowledgebaseService;
    private final ObjectMapper objectMapper;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public KnowledgeTreeNode createNode(KnowledgeTreeNodeCreateReq createReq) {
        KnowledgeTreeNode knowledgeTreeNode = KnowledgeTreeNodeSM.INSTANCE.createDTO2Entity(createReq);
        
        // 初始化填充默认值
        fillDefaultValues(knowledgeTreeNode);
        
        String nodeType = null;

        // 处理节点配置（智能推荐或自定义）
        nodeType = handleNodeConfiguration(createReq, knowledgeTreeNode);
        log.info("创建知识树节点，节点名称: {}, 节点类型: {}", knowledgeTreeNode.getName(), nodeType);

        // 创建RAGFlow数据集
        RAGFlowDatasetCreateResp resp = createRAGFlowDataset(nodeType, createReq);

        // 检查API调用结果并处理
        if (resp != null && resp.getCode() == 0 && resp.getData() != null) {
            // 设置节点层级
            setNodeLevel(knowledgeTreeNode);

            // 设置节点属性
            setNodeAttributes(knowledgeTreeNode, resp, nodeType, createReq);

            // 保存到数据库
            if (baseMapper.insert(knowledgeTreeNode) != 1) {
                log.error("知识树节点创建失败，ID: {}, RAGFlow数据集ID: {}", knowledgeTreeNode.getId(), knowledgeTreeNode.getKdbId());
                throw new RuntimeException("知识树节点创建失败");
            }
            log.info("知识树节点创建成功，ID: {}, RAGFlow数据集ID: {}, 类型: {}, 自动选择: {}", 
                    knowledgeTreeNode.getId(), knowledgeTreeNode.getKdbId(), nodeType, createReq.getAutoSelect());
            return knowledgeTreeNode;
        } else {
            handleRAGFlowDatasetCreationFailure(resp);
            return null; // 不会执行到这里，因为上面的方法会抛出异常
        }
    }

    /**
     * 处理节点配置（智能推荐或自定义）
     * 
     * @param createReq 创建请求
     * @param knowledgeTreeNode 知识树节点
     * @return 节点类型
     */
    private String handleNodeConfiguration(KnowledgeTreeNodeCreateReq createReq, KnowledgeTreeNode knowledgeTreeNode) {
        String nodeType = null;
        String autoSelect = createReq.getAutoSelect();
        
        if ("ai".equals(autoSelect) && knowledgeTreeNode.getPid() != null && !knowledgeTreeNode.getPid().isEmpty()) {
            log.info("节点[{}]设置为智能推荐(ai)，将从父节点继承切片配置", knowledgeTreeNode.getName());
            
            // 获取父节点信息
            KnowledgeTreeNode pNode = lambdaQuery()
                    .eq(KnowledgeTreeNode::getId, knowledgeTreeNode.getPid())
                    .one();
            
            if (pNode != null) {
                // 从父节点继承配置，确保处理null值
                nodeType = pNode.getType() != null ? pNode.getType() : "general";
                createReq.setType(nodeType);
                
                // 处理分隔符
                String delimiter = pNode.getDelimiter() != null ? pNode.getDelimiter() : "\\n!?;。；！？";
                createReq.setDelimiter(delimiter);
                
                // 处理分块token数量
                Integer chunkTokenNum = pNode.getChunkTokenNum() != null ? pNode.getChunkTokenNum() : 512;
                createReq.setChunkTokenNum(chunkTokenNum);

                Integer autoKeywords = createReq.getAutoKeywords();
                Integer autoQuestions = createReq.getAutoQuestions();
                
                log.info("从父节点[{}]继承配置：类型={}, 分隔符={}, 分块token数={}, 自动关键词={}, 自动问题={}",
                        pNode.getId(), nodeType, delimiter, chunkTokenNum, autoKeywords, autoQuestions);
            } else if ("0".equals(knowledgeTreeNode.getPid())) {
                // 如果父节点是根节点，则使用默认配置
                log.info("节点[{}]的父节点是根节点，将使用默认配置", knowledgeTreeNode.getName());
                nodeType = "general";
                createReq.setType("general");
                createReq.setDelimiter("\\n!?;。；！？");
                createReq.setChunkTokenNum(512);
                createReq.setAutoQuestions(createReq.getAutoQuestions());
                createReq.setAutoKeywords(createReq.getAutoKeywords());
            } else {
                log.error("无法找到父节点[{}]", knowledgeTreeNode.getPid());
                throw new RuntimeException("无法找到父节点");
            }
        } else {
            log.info("节点[{}]设置为自定义配置(custom)或未指定，将使用提供的配置", knowledgeTreeNode.getName());
            
            // 确保请求中的属性有默认值
            if (createReq.getDelimiter() == null || createReq.getDelimiter().isEmpty()) {
                createReq.setDelimiter("\\n!?;。；！？");
            }
            
            if (createReq.getChunkTokenNum() == null || createReq.getChunkTokenNum() <= 0) {
                createReq.setChunkTokenNum(512);
            }
            
            if (createReq.getAutoKeywords() == null) {
                createReq.setAutoKeywords(5);
            }
            
            if (createReq.getAutoQuestions() == null) {
                createReq.setAutoQuestions(2);
            }
        }

        // 获取节点类型
        if (nodeType == null || nodeType.isEmpty()) {
            nodeType = createReq.getType();
            // 如果类型仍然为null，设置默认值
            if (nodeType == null || nodeType.isEmpty()) {
                nodeType = "general";
                createReq.setType(nodeType);
            }
        }
        
        return nodeType;
    }

    /**
     * 创建RAGFlow数据集
     * 
     * @param nodeType 节点类型
     * @param createReq 创建请求
     * @return RAGFlow数据集创建响应
     */
    private RAGFlowDatasetCreateResp createRAGFlowDataset(String nodeType, KnowledgeTreeNodeCreateReq createReq) {
        RAGFlowDatasetCreateResp resp;
        
        // 根据节点类型创建不同类型的数据集
        if (nodeType != null) {
            switch (nodeType.toLowerCase()) {
                case "general":
                case "通用":
                    resp = ragFlowDBAPIService.createGeneralDataset(
                            createReq.getName(),
                            createReq.getDescription(),
                            "team",
                            createReq.getDelimiter(),
                            createReq.getChunkTokenNum(),
                            createReq.getAutoKeywords(),
                            createReq.getAutoQuestions());
                    break;
                case "laws":
                case "法律":
                    resp = ragFlowDBAPIService.createLawsDataset(
                            createReq.getName(),
                            createReq.getDescription(),
                            "team",
                            createReq.getDelimiter(),
                            createReq.getChunkTokenNum(),
                            createReq.getAutoKeywords(),
                            createReq.getAutoQuestions());
                    break;
                case "paper":
                case "论文":
                    resp = ragFlowDBAPIService.createPaperDataset(
                            createReq.getName(),
                            createReq.getDescription(),
                            "team",
                            createReq.getDelimiter(),
                            createReq.getChunkTokenNum(),
                            createReq.getAutoKeywords(),
                            createReq.getAutoQuestions());
                    break;
                case "book":
                case "书籍":
                    resp = ragFlowDBAPIService.createBookDataset(
                            createReq.getName(),
                            createReq.getDescription(),
                            "team",
                            createReq.getDelimiter(),
                            createReq.getChunkTokenNum(),
                            createReq.getAutoKeywords(),
                            createReq.getAutoQuestions());
                    break;
                case "qa":
                case "问答":
                case "问答对":
                    resp = ragFlowDBAPIService.createQADataset(
                            createReq.getName(),
                            createReq.getDescription(),
                            "team",
                            createReq.getDelimiter(),
                            createReq.getChunkTokenNum(),
                            createReq.getAutoKeywords(),
                            createReq.getAutoQuestions());
                    break;
                default:
                    // 默认创建通用类型数据集
                    log.info("未知节点类型: {}，将创建通用类型数据集", nodeType);
                    resp = ragFlowDBAPIService.createGeneralDataset(
                            createReq.getName(),
                            createReq.getDescription(),
                            "team",
                            createReq.getDelimiter(),
                            createReq.getChunkTokenNum(),
                            createReq.getAutoKeywords(),
                            createReq.getAutoQuestions());
                    break;
            }
        } else {
            // 如果未指定类型，默认创建通用类型数据集
            log.info("未指定节点类型，将创建通用类型数据集");
            resp = ragFlowDBAPIService.createGeneralDataset(
                    createReq.getName(),
                    createReq.getDescription(),
                    "team",
                    createReq.getDelimiter(),
                    createReq.getChunkTokenNum(),
                    createReq.getAutoKeywords(),
                    createReq.getAutoQuestions());
        }
        
        return resp;
    }

    /**
     * 设置节点层级
     * 
     * @param knowledgeTreeNode 知识树节点
     */
    private void setNodeLevel(KnowledgeTreeNode knowledgeTreeNode) {
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
    }

    /**
     * 设置节点属性
     * 
     * @param knowledgeTreeNode 知识树节点
     * @param resp RAGFlow数据集创建响应
     * @param nodeType 节点类型
     * @param createReq 创建请求
     */
    private void setNodeAttributes(KnowledgeTreeNode knowledgeTreeNode, RAGFlowDatasetCreateResp resp, String nodeType, KnowledgeTreeNodeCreateReq createReq) {
        // 设置数据库记录的相关字段
        knowledgeTreeNode.setKdbId(resp.getData().getId());
        knowledgeTreeNode.setRagflowName(resp.getData().getName());
        knowledgeTreeNode.setEmbeddingsModel(resp.getData().getEmbeddingModel());
        knowledgeTreeNode.setType(nodeType); // 设置节点类型
        knowledgeTreeNode.setAutoSelect(createReq.getAutoSelect()); // 设置自动选择类型
        
        // 从API响应中获取实际使用的配置参数
        if (resp.getData().getParserConfig() != null) {
            // 设置分隔符
            knowledgeTreeNode.setDelimiter(resp.getData().getParserConfig().getDelimiter());
            
            // 设置分块token数量
            knowledgeTreeNode.setChunkTokenNum(resp.getData().getParserConfig().getChunkTokenNum());
            
            // API响应中没有autoKeywords和autoQuestions字段，使用请求中的值
            if (createReq.getAutoKeywords() != null) {
                knowledgeTreeNode.setAutoKeywords(createReq.getAutoKeywords());
            } else {
                knowledgeTreeNode.setAutoKeywords(5);
            }
            
            if (createReq.getAutoQuestions() != null) {
                knowledgeTreeNode.setAutoQuestions(createReq.getAutoQuestions());
            } else {
                knowledgeTreeNode.setAutoQuestions(2);
            }
        } else {
            // 如果API响应中没有parser_config，则使用请求中的值
            // 设置分隔符
            if(createReq.getDelimiter() != null) {
                knowledgeTreeNode.setDelimiter(createReq.getDelimiter());
            } else {
                knowledgeTreeNode.setDelimiter("\\n!?;。；！？");
            }

            // 设置分块token数量
            if (createReq.getChunkTokenNum() != null) {
                knowledgeTreeNode.setChunkTokenNum(createReq.getChunkTokenNum());
            } else {
                knowledgeTreeNode.setChunkTokenNum(512);
            }

            // 设置自动关键词提取
            if (createReq.getAutoKeywords() != null) {
                knowledgeTreeNode.setAutoKeywords(createReq.getAutoKeywords());
            } else {
                knowledgeTreeNode.setAutoKeywords(5);
            }

            // 设置自动问题提取
            if (createReq.getAutoQuestions() != null) {
                knowledgeTreeNode.setAutoQuestions(createReq.getAutoQuestions());
            } else {
                knowledgeTreeNode.setAutoQuestions(2);
            }
        }

        // 设置创建和更新时间
        Date now = new Date();
        knowledgeTreeNode.setDocumentNum(0);
        knowledgeTreeNode.setDocumentSize(0L);
        knowledgeTreeNode.setTokenNum(0L);
        knowledgeTreeNode.setChunkNum(0);
        knowledgeTreeNode.setCreateTime(now);
        knowledgeTreeNode.setUpdateTime(now);
        
        // 填充其他可能为null的属性
        fillDefaultValues(knowledgeTreeNode);
    }
    
    /**
     * 为KnowledgeTreeNode中的null或空属性填充默认值
     * 
     * @param node 知识树节点
     */
    private void fillDefaultValues(KnowledgeTreeNode node) {
        // 设置排序顺序默认值
        if (node.getSortOrder() == null) {
            node.setSortOrder(0);
        }
        
        // 设置描述默认值
        if (node.getDescription() == null) {
            node.setDescription("");
        }
        
        // 设置自动选择默认值
        if (node.getAutoSelect() == null) {
            node.setAutoSelect("custom");
        }
        
        // 设置节点类型默认值
        if (node.getType() == null) {
            node.setType("general");
        }
        
        // 设置分隔符默认值
        if (node.getDelimiter() == null) {
            node.setDelimiter("\\n!?;。；！？");
        }
        
        // 设置分块token数量默认值
        if (node.getChunkTokenNum() == null) {
            node.setChunkTokenNum(512);
        }

        // 设置自动关键词提取默认值
        if (node.getAutoKeywords() == null) {
            node.setAutoKeywords(5);
        }

        // 设置自动问题提取默认值
        if (node.getAutoQuestions() == null) {
            node.setAutoQuestions(2);
        }
        
        // 确保统计数据初始化为0
        if (node.getDocumentNum() == null) {
            node.setDocumentNum(0);
        }
        
        if (node.getDocumentSize() == null) {
            node.setDocumentSize(0L);
        }
        
        if (node.getTokenNum() == null) {
            node.setTokenNum(0L);
        }
        
        if (node.getChunkNum() == null) {
            node.setChunkNum(0);
        }
    }

    /**
     * 处理RAGFlow数据集创建失败
     * 
     * @param resp RAGFlow数据集创建响应
     */
    private void handleRAGFlowDatasetCreationFailure(RAGFlowDatasetCreateResp resp) {
        log.error("创建RAGFlow数据集失败: {}", resp != null ? resp.getMessage() : "响应为空");
        // 删除RAGFlow数据集
        if (resp != null && resp.getData() != null) {
            RAGFlowDatasetDeleteReq deleteReq = new RAGFlowDatasetDeleteReq();
            deleteReq.setIds(Arrays.asList(resp.getData().getId()));
            ragFlowDBAPIService.deleteDatasets(deleteReq);
        }
        throw new RuntimeException("创建RAGFlow数据集失败: " + (resp != null ? resp.getMessage() : "响应为空"));
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
     *
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
     *
     * @param parentId 父节点ID
     * @param idList   用于存储结果的列表
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
        root.setType("general");

        // 构建节点映射，用于快速查找
        Map<String, KnowledgeTreeNodeDTO> nodeMap = new HashMap<>();
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

    /**
     * 节点统计信息（内部类）
     */
    @Data
    private static class NodeStatistics {
        private Integer documentNum = 0;
        private Long documentSize = 0L;
        private Long tokenNum = 0L;
        private Integer chunkNum = 0;
        private String type;
        private String parserConfig;

        public void add(NodeStatistics other) {
            if (other != null) {
                this.documentNum += other.documentNum != null ? other.documentNum : 0;
                this.documentSize += other.documentSize != null ? other.documentSize : 0L;
                this.tokenNum += other.tokenNum != null ? other.tokenNum : 0L;
                this.chunkNum += other.chunkNum != null ? other.chunkNum : 0;
            }
        }
    }

    @Override
    public void updateAllNodesStatistic() {
        log.info("开始更新所有节点的统计信息");
        try {
            // 获取所有根节点
            List<KnowledgeTreeNode> rootNodes = lambdaQuery()
                    .eq(KnowledgeTreeNode::getPid, "0")
                    .list();

            // 递归更新每个根节点及其子节点的统计信息
            for (KnowledgeTreeNode rootNode : rootNodes) {
                updateNode(rootNode.getId());
            }
            log.info("所有节点的统计信息更新完成");
        } catch (Exception e) {
            log.error("更新所有节点统计信息失败", e);
            throw new RuntimeException("更新所有节点统计信息失败: " + e.getMessage());
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateNode(String nodeId) {
        updateNodeStatistics(nodeId);
    }

    /**
     * 递归更新节点统计信息的内部方法
     */
    private NodeStatistics updateNodeStatistics(String nodeId) {
        log.info("开始更新节点[{}]的统计信息", nodeId);
        try {
            // 获取节点信息
            KnowledgeTreeNode node = getById(nodeId);
            if (node == null) {
                log.error("找不到指定ID的知识树节点, 节点ID: {}", nodeId);
                return null;
            }

            Knowledgebase knowledgebase = knowledgebaseService.getById(node.getKdbId());

            // 获取该节点自身的统计信息
            NodeStatistics ownStats = new NodeStatistics();
            if (node.getKdbId() != null && !node.getKdbId().isEmpty()) {
                ownStats.setDocumentNum(knowledgebase.getDocNum());
                ownStats.setDocumentSize(documentService.countDocumentSizeByKbId(node.getKdbId()));
                ownStats.setTokenNum(knowledgebase.getTokenNum());
                ownStats.setChunkNum(knowledgebase.getChunkNum());
                ownStats.setParserConfig(knowledgebase.getParserConfig());
                ownStats.setType(getParserType(knowledgebase.getParserId()));
            }

            // 获取所有子节点
            List<KnowledgeTreeNode> childNodes = lambdaQuery()
                    .eq(KnowledgeTreeNode::getPid, nodeId)
                    .list();

            // 如果没有子节点，则统计信息就是自身的统计信息
            if (childNodes.isEmpty()) {
                // 更新节点的统计信息
                updateNodeWithStats(node, ownStats);
                log.info("节点[{}]没有子节点，统计信息设为自身统计数据：文档数: {}, 文档大小: {}, 词元数: {}, 文本块数: {}, 解析设置: {}, 类型: {}",
                        nodeId, ownStats.getDocumentNum(), ownStats.getDocumentSize(),
                        ownStats.getTokenNum(), ownStats.getChunkNum(), ownStats.getParserConfig(), ownStats.getType());
                return ownStats;
            }

            // 递归计算所有子节点的统计信息
            NodeStatistics childrenStats = new NodeStatistics();
            for (KnowledgeTreeNode childNode : childNodes) {
                NodeStatistics childStats = updateNodeStatistics(childNode.getId());
                if (childStats != null) {
                    childrenStats.add(childStats);
                }
            }

            // 计算总统计信息（自身统计信息 + 子节点统计信息）
            NodeStatistics totalStats = new NodeStatistics();
            totalStats.add(ownStats);
            totalStats.add(childrenStats);
            totalStats.setType(getParserType(knowledgebase.getParserId()));
            totalStats.setParserConfig(knowledgebase.getParserConfig());

            // 更新节点的统计信息
            updateNodeWithStats(node, totalStats);

            log.info("节点[{}]的统计信息更新完成，自身统计：文档数: {}, 文档大小: {}, 词元数: {}, 文本块数: {}, " +
                            "子节点统计：文档数: {}, 文档大小: {}, 词元数: {}, 文本块数: {}, " +
                            "总计：文档数: {}, 文档大小: {}, 词元数: {}, 文本块数: {}, 解析设置: {}",
                    nodeId,
                    ownStats.getDocumentNum(), ownStats.getDocumentSize(),
                    ownStats.getTokenNum(), ownStats.getChunkNum(),
                    childrenStats.getDocumentNum(), childrenStats.getDocumentSize(),
                    childrenStats.getTokenNum(), childrenStats.getChunkNum(),
                    totalStats.getDocumentNum(), totalStats.getDocumentSize(),
                    totalStats.getTokenNum(), totalStats.getChunkNum(),
                    totalStats.getParserConfig());

            return totalStats;
        } catch (Exception e) {
            log.error("更新节点[{}]统计信息失败", nodeId, e);
            throw new RuntimeException("更新节点统计信息失败: " + e.getMessage());
        }
    }

    private String getParserType(String parserId) {
        if (parserId == null || parserId.isEmpty()) {
            return "general";
        }
        switch (parserId) {
            case "manual":
                return "laws";
            case "paper":
                return "paper";
            case "book":
                return "book";
            case "qa":
                return "qa";
            case "naive":
                return "general";
            default:
                return "general";
        }
    }

    /**
     * 使用统计信息更新节点
     */
    private void updateNodeWithStats(KnowledgeTreeNode node, NodeStatistics stats) throws JsonProcessingException {
        node.setDocumentNum(stats.getDocumentNum());
        node.setDocumentSize(stats.getDocumentSize());
        node.setTokenNum(stats.getTokenNum());
        node.setChunkNum(stats.getChunkNum());
        node.setType(stats.getType());

        if (stats.getParserConfig() != null) {
            JsonNode jsonNode = objectMapper.readTree(stats.getParserConfig());
            int chunkTokenNum = jsonNode.get("chunk_token_num") == null ? 0 : jsonNode.get("chunk_token_num").asInt();
            String delimiter = jsonNode.get("delimiter") == null ? "" : jsonNode.get("delimiter").asText();
            int autoKeywords = jsonNode.get("auto_keywords") == null ? 0 : jsonNode.get("auto_keywords").asInt();
            int autoQuestions = jsonNode.get("auto_questions") == null ? 0 : jsonNode.get("auto_questions").asInt();
            node.setChunkTokenNum(chunkTokenNum);
            node.setDelimiter(delimiter);
            node.setAutoKeywords(autoKeywords);
            node.setAutoQuestions(autoQuestions);
        }

        updateById(node);
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

    @Override
    public KnowledgeTreeStatisticDTO getTreeNodeStatisticDTO(String pid) {
        log.info("开始获取节点[{}]的统计信息", pid);

        KnowledgeTreeStatisticDTO result = new KnowledgeTreeStatisticDTO();

        try {
            // 如果pid为0，创建一个虚拟根节点
            if ("0".equals(pid)) {
                result.setId("0");
                result.setName("全部");
                result.setDescription("全部");

                // 获取所有顶级节点作为子节点
                List<KnowledgeTreeNode> childNodes = lambdaQuery()
                        .eq(KnowledgeTreeNode::getPid, "0")
                        .list();

                // 转换子节点信息
                List<KnowledgeTreeStatisticDTO> children = new ArrayList<>();
                int totalDocumentNum = 0;
                long totalDocumentSize = 0L;
                long totalTokenNum = 0L;
                int totalChunkNum = 0;

                for (KnowledgeTreeNode node : childNodes) {
                    KnowledgeTreeStatisticDTO childDTO = KnowledgeTreeNodeSM.INSTANCE.entity2StatisticDTO(node);
                    // 检查子节点是否有子节点
                    childDTO.setHasChildren(hasChildren(node.getId()));
                    children.add(childDTO);

                    // 累加统计信息
                    totalDocumentNum += node.getDocumentNum() != null ? node.getDocumentNum() : 0;
                    totalDocumentSize += node.getDocumentSize() != null ? node.getDocumentSize() : 0L;
                    totalTokenNum += node.getTokenNum() != null ? node.getTokenNum() : 0L;
                    totalChunkNum += node.getChunkNum() != null ? node.getChunkNum() : 0;
                }

                // 设置根节点的统计信息
                result.setChildren(children);
                result.setDocumentNum(totalDocumentNum);
                result.setDocumentSize(totalDocumentSize);
                result.setTokenNum(totalTokenNum);
                result.setChunkNum(totalChunkNum);
                // 根节点只要有子节点就设置为true
                result.setHasChildren(!children.isEmpty());

            } else {
                // 获取当前节点信息
                KnowledgeTreeNode currentNode = getById(pid);
                if (currentNode == null) {
                    log.error("找不到指定ID的知识树节点, 节点ID: {}", pid);
                    return null;
                }

                // 转换当前节点信息
                result = KnowledgeTreeNodeSM.INSTANCE.entity2StatisticDTO(currentNode);

                // 获取直接子节点
                List<KnowledgeTreeNode> childNodes = lambdaQuery()
                        .eq(KnowledgeTreeNode::getPid, pid)
                        .list();

                // 转换子节点信息
                List<KnowledgeTreeStatisticDTO> children = new ArrayList<>();
                for (KnowledgeTreeNode node : childNodes) {
                    KnowledgeTreeStatisticDTO childDTO = KnowledgeTreeNodeSM.INSTANCE.entity2StatisticDTO(node);
                    // 检查子节点是否有子节点
                    childDTO.setHasChildren(hasChildren(node.getId()));
                    children.add(childDTO);
                }

                result.setChildren(children);
                // 设置当前节点是否有子节点
                result.setHasChildren(!children.isEmpty());
            }

            log.info("节点[{}]的统计信息获取完成", pid);
            return result;

        } catch (Exception e) {
            log.error("获取节点[{}]的统计信息失败", pid, e);
            throw new RuntimeException("获取节点统计信息失败: " + e.getMessage());
        }
    }

    /**
     * 检查节点是否有子节点
     */
    private boolean hasChildren(String nodeId) {
        return lambdaQuery()
                .eq(KnowledgeTreeNode::getPid, nodeId)
                .exists();
    }
}




