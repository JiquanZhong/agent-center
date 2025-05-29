package com.diit.ds.pojo.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.diit.ds.pojo.enums.AgentSourceEnum;
import com.diit.ds.pojo.enums.AgentStatus;
import com.diit.ds.pojo.vo.AgentsVo;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * @author yjxbz
 */
@Data
@TableName("agents")
@Schema(description = "智能体实体类")
public class Agents {

    @TableId(type = IdType.AUTO)
    @Schema(description = "主键")
    private Integer id;

    @Schema(description = "智能体来源：DIFY;LOCAL;COZE")
    private AgentSourceEnum source;

    @Schema(description = "智能体名称")
    private String name;

    @Schema(description = "图标")
    private String icon;

    @Schema(description = "状态")
    private AgentStatus status;

    @Schema(description = "智能体摘要")
    private String description;

    @Schema(description = "发布日期")
    private LocalDateTime publishDate;

    @Schema(description = "创建日期")
    private LocalDateTime createDate;

    @Schema(description = "更新日期")
    private LocalDateTime updateDate;

    @Schema(description = "创建人Id")
    private String createUser;

    @Schema(description = "更新人Id")
    private String updateUser;

    @Schema(description = "来源app_id，目前只有DIFY的智能体需要这个")
    private String sourceId;

    @Schema(description = "标签列表")
    private Integer tag;

    @Schema(description = "智能体访问量")
    private Long browserCount;

    @Schema(description = "智能体收藏量")
    private Long favoriteCount;

    @Schema(description = "公开访问url")
    private String publishUrl;

    @Schema(description = "背景图")
    private String background;

    @Schema(description = "apiKey")
    private String apiKey;
}
