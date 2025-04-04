package com.diit.ds.domain.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.util.Date;
import lombok.Data;

/**
 * 知识中心 树节点表
 * @TableName knowledge_tree_node
 */
@TableName(value = "knowledge_tree_node", autoResultMap = true)
@Data
public class KnowledgeTreeNode {
    /**
     * 主键ID
     */
    @TableId(value = "id", type = IdType.ASSIGN_UUID)
    private String id;

    /**
     * ragflow中的kdb_id
     */
    @TableField("kdb_id")
    private String kdbId;

    /**
     * 父节点id
     */
    @TableField("pid")
    private String pid;

    /**
     * 节点层级 从1开始 0代表全部节点 库中不存
     */
    @TableField("level")
    private Integer level;

    /**
     * 节点名称
     */
    @TableField("name")
    private String name;

    /**
     * ragflow中对应的文件名称
     */
    @TableField("ragflow_name")
    private String ragflowName;

    /**
     * ragflow中使用的嵌入模型id
     */
    @TableField("embeddings_model")
    private String embeddingsModel;

    /**
     * 节点排序
     */
    @TableField("sort_order")
    private Integer sortOrder;

    /**
     * 创建时间
     */
    @TableField("create_time")
    private Date createTime;

    /**
     * 更新时间
     */
    @TableField("update_time")
    private Date updateTime;

    /**
     * 节点描述
     */
    @TableField("description")
    private String description;

    /**
     * 文档数量
     */
    @TableField("document_num")
    private Integer documentNum;
}