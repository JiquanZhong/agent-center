package com.diit.ds.domain.pojo.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.util.Date;

/**
 * RAG工作流文档聚合表
 * @TableName workflow_doc_agg
 */
@TableName(value ="workflow_doc_agg")
@Data
public class WorkflowDocAgg {
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
     * 文档ID
     */
    @TableField(value = "doc_id")
    private String docId;

    /**
     * 文档名称
     */
    @TableField(value = "doc_name")
    private String docName;

    /**
     * 文档出现次数
     */
    @TableField(value = "count")
    private Integer count;

    /**
     * 
     */
    @TableField(value = "create_time")
    private Date createTime;

    @TableField(value = "type")
    private String type;
}