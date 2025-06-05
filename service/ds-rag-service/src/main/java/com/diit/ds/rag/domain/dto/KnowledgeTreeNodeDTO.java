package com.diit.ds.rag.domain.dto;

import lombok.Data;

import java.util.List;

/**
 * 知识中心 树节点表
 * @TableName knowledge_tree_node
 */
@Data
public class KnowledgeTreeNodeDTO {
    /**
     * 主键ID
     */
    private String id;

    /**
     * ragflow中的kdb_id
     */
    private String kdbId;

    /**
     * 节点名称
     */
    private String name;

    /**
     * 节点排序
     */
    private Integer sortOrder;

    /**
     * 节点描述
     */
    private String description;

    /**
     * 节点类型
     */
    private String type;

    /**
     * 是否智能推荐：ai || custom
     */
    private String autoSelect;

    /**
     * 分隔符，默认为"\n!?;。；！？"
     */
    private String delimiter;

    /**
     * 分块token数量，默认为1024
     */
    private Integer chunkTokenNum;

    /**
     * 是否进行自动关键词提取
     */
    private Integer autoKeywords;

    /**
     * 是否进行问题提取
     */
    private Integer autoQuestions;

    /**
     * 文档数量
     */
    private Integer documentNum;

    /**
     * 子节点
     */
    private List<KnowledgeTreeNodeDTO> children;
}