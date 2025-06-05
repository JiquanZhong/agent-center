package com.diit.ds.domain.pojo.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.diit.ds.domain.pojo.enums.SubReviewResultEnums;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * @author yjxbz
 */
@Data
@TableName("agents_sub_review") // 替换为实际表名
@Schema(name = "AgentsSubReview", description = "智能体申请与审核记录实体")
public class AgentsSubReview {

    @TableId(type = IdType.AUTO)
    @Schema(description = "主键")
    private Integer id;

    @Schema(description = "智能体id")
    private Integer agentId;

    @Schema(description = "申请人id")
    private String subUserId;

    @Schema(description = "申请时间")
    private LocalDateTime subDate;

    @Schema(description = "审核人id")
    private String reviewUserId;

    @Schema(description = "审核时间")
    private LocalDateTime reviewDate;

    @Schema(description = "审核结果 REVIEWING审核中;PASS审核通过;UNPASS审核未通过")
    private SubReviewResultEnums results;

    @Schema(description = "申请描述")
    private String subDescription;
}
