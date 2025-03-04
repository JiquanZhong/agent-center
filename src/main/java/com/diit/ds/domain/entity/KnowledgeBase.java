package com.diit.ds.domain.entity;

import java.util.Date;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

/**
 * 知识库表
 * @TableName htyy_knowledge_base
 */
@TableName(value ="htyy_knowledge_base")
@Data
public class KnowledgeBase {
    /**
     * 知识ID
     */
    @TableId
    private String id;

    /**
     * 知识标题
     */
    private String title;

    /**
     * 知识内容
     */
    private String content;

    /**
     * 创建时间
     */
    private Date createdAt;
}