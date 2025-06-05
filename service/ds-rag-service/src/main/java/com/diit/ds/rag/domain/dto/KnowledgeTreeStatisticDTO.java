package com.diit.ds.rag.domain.dto;

import lombok.Data;

import java.util.List;

/**
 * 知识中心 树统计信息
 * @TableName knowledge_tree_node
 */
@Data
public class KnowledgeTreeStatisticDTO {
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
     * 文档数量
     */
    private Integer documentNum;


    /**
     * 文档大小
     */
    private Long documentSize;

    /**
     * 词元数量
     */
    private Long tokenNum;

    /**
     * 文本块数量
     */
    private Integer chunkNum;

    /**
     * 是否可以下钻
     */
    private Boolean hasChildren;

    /**
     * 子节点
     */
    private List<KnowledgeTreeStatisticDTO> children;
}