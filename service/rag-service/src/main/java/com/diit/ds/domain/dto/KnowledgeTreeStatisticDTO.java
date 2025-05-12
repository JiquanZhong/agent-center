package com.diit.ds.domain.dto;

import com.baomidou.mybatisplus.annotation.TableField;
import com.fasterxml.jackson.annotation.JsonProperty;
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
    @JsonProperty("kdb_id")
    private String kdbId;

    /**
     * 节点名称
     */
    private String name;

    /**
     * 节点排序
     */
    @JsonProperty("sort_order")
    private Integer sortOrder;

    /**
     * 节点描述
     */
    private String description;

    /**
     * 文档数量
     */
    @JsonProperty("document_num")
    private Integer documentNum;


    /**
     * 文档大小
     */
    @JsonProperty("document_size")
    private Long documentSize;

    /**
     * 词元数量
     */
    @JsonProperty("token_num")
    private Long tokenNum;

    /**
     * 文本块数量
     */
    @JsonProperty("chunk_num")
    private Integer chunkNum;

    /**
     * 是否可以下钻
     */
    @JsonProperty("has_children")
    private Boolean hasChildren;

    /**
     * 子节点
     */
    private List<KnowledgeTreeStatisticDTO> children;
}