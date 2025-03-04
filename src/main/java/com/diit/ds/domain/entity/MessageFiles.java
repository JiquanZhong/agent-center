package com.diit.ds.domain.entity;

import java.util.Date;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

/**
 * 对话消息的文件信息表
 * @TableName htyy_message_files
 */
@TableName(value ="htyy_message_files")
@Data
public class MessageFiles {
    /**
     * 文件ID
     */
    @TableId
    private String id;

    /**
     * 所属消息ID
     */
    private String messageId;

    /**
     * 文件存储URL
     */
    private String fileUrl;

    /**
     * 文件类型（如png, pdf等）
     */
    private String fileType;

    /**
     * 文件大小（字节）
     */
    private Integer fileSize;

    /**
     * 创建时间
     */
    private Date createdAt;
}