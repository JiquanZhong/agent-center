package com.diit.ds.domain.dto;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.util.Date;
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
     * 子节点
     */
    private List<KnowledgeTreeNodeDTO> children;
}