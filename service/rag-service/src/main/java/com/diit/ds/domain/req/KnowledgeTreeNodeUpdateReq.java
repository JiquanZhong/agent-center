package com.diit.ds.domain.req;

import com.baomidou.mybatisplus.annotation.TableField;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

/**
 * 知识中心 树节点表
 * @TableName knowledge_tree_node
 */
@Data
public class KnowledgeTreeNodeUpdateReq {

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
}