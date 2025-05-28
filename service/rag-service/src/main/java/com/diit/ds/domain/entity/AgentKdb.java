package com.diit.ds.domain.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.util.Date;
import lombok.Data;

/**
 * 智能体 知识库id关联表
 * @TableName agent_kdb
 */
@TableName(value ="agent_kdb")
@Data
public class AgentKdb {
    /**
     * 
     */
    @TableId(value = "id")
    private String id;

    /**
     * 
     */
    @TableField(value = "app_id")
    private String appId;

    /**
     * 知识库id，以,分割
     */
    @TableField(value = "kdb_id")
    private String kdbId;

    /**
     * 
     */
    @TableField(value = "create_time")
    private Date createTime;

    /**
     * 
     */
    @TableField(value = "update_time")
    private Date updateTime;
}