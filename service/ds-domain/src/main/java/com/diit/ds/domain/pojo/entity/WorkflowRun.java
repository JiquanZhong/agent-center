package com.diit.ds.domain.pojo.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.util.Date;

/**
 * RAG工作流运行主表
 * @TableName workflow_run
 */
@TableName(value ="workflow_run")
@Data
public class WorkflowRun {

    /**
     * 
     */
    @TableId(value = "id")
    private Long id;

    /**
     * 工作流运行ID
     */
    @TableField(value = "work_flow_run_id")
    private String workFlowRunId;

    /**
     * 响应码
     */
    @TableField(value = "code")
    private Integer code;

    /**
     * 响应消息
     */
    @TableField(value = "message")
    private String message;

    /**
     * 结果总数
     */
    @TableField(value = "total")
    private Integer total;

    /**
     * 查询文本
     */
    @TableField(value = "query_text")
    private String queryText;

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