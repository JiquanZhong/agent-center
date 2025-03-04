package com.diit.ds.domain.entity;

import java.util.Date;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

/**
 * 日志表
 * @TableName htyy_logs
 */
@TableName(value ="htyy_logs")
@Data
public class Logs {
    /**
     * 日志ID
     */
    @TableId
    private String id;

    /**
     * 用户ID
     */
    private String userId;

    /**
     * 用户操作内容
     */
    private String action;

    /**
     * 创建时间
     */
    private Date createdAt;
}