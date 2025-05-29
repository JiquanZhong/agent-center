package com.diit.ds.pojo.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * @author yjxbz
 */
@Data
@TableName("agents_favorite") // 替换为真实表名
@Schema(name = "AgentsFavorite", description = "智能体收藏")
public class AgentsFavorite {

    @TableId(type = IdType.AUTO)
    @Schema(description = "主键")
    private Integer id;

    @Schema(description = "用户id")
    private String userId;

    @Schema(description = "智能体id")
    private Integer agentId;

    @Schema(description = "收藏时间")
    private LocalDateTime createDate;
}
