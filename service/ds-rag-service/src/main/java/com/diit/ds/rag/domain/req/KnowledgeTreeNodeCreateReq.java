package com.diit.ds.rag.domain.req;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.util.Date;

/**
 * 知识中心 树节点表
 * @TableName knowledge_tree_node
 */
@Data
public class KnowledgeTreeNodeCreateReq {

    /**
     * 父节点id
     */
    private String pid;

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
}