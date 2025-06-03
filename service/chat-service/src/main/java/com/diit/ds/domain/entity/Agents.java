package com.diit.ds.domain.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.util.Date;
import lombok.Data;

/**
 * 智能体表
 * @TableName agents
 */
@TableName(value ="agents")
@Data
public class Agents {
    /**
     * 主键
     */
    @TableId(value = "id")
    private String id;

    /**
     * 智能体来源：DIFY;LOCAL;COZE
     */
    @TableField(value = "source")
    private String source;

    /**
     * 智能体名称
     */
    @TableField(value = "name")
    private String name;

    /**
     * 图标
     */
    @TableField(value = "icon")
    private String icon;

    /**
     * 状态
     */
    @TableField(value = "status")
    private String status;

    /**
     * 智能体描述
     */
    @TableField(value = "description")
    private String description;

    /**
     * 发布日期
     */
    @TableField(value = "publish_date")
    private Date publishDate;

    /**
     * 创建日期
     */
    @TableField(value = "create_date")
    private Date createDate;

    /**
     * 更新日期
     */
    @TableField(value = "update_date")
    private Date updateDate;

    /**
     * 创建人id
     */
    @TableField(value = "create_user")
    private String createUser;

    /**
     * 更新人id
     */
    @TableField(value = "update_user")
    private String updateUser;

    /**
     * 来源的app_id,目前只有DIFY的智能体需要这个
     */
    @TableField(value = "source_id")
    private String sourceId;

    /**
     * 标签列表
     */
    @TableField(value = "tag")
    private Integer tag;

    /**
     * 智能体访问量
     */
    @TableField(value = "browser_count")
    private Long browserCount;

    /**
     * 智能体收藏量
     */
    @TableField(value = "favorite_count")
    private Long favoriteCount;

    /**
     * 公开访问url
     */
    @TableField(value = "publish_url")
    private String publishUrl;

    /**
     * 
     */
    @TableField(value = "api_key")
    private String apiKey;

    /**
     * 
     */
    @TableField(value = "background")
    private String background;
}