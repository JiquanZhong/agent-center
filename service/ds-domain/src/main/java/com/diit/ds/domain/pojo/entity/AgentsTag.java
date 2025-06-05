package com.diit.ds.domain.pojo.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * @author yjxbz
 */
@Data
@TableName("agents_tag")
@Schema(description = "智能体标签实体类")
@AllArgsConstructor
@NoArgsConstructor
public class AgentsTag {

    @TableId(type = IdType.AUTO)
    @Schema(description = "主键")
    private Integer id;

    @Schema(description = "标签名称")
    private String name;

    @Schema(description = "创建日期")
    private LocalDateTime createTime;

    @Schema(description = "创建人")
    private String createUser;

    @Schema(description = "排序字段")
    private Integer orders;
}
