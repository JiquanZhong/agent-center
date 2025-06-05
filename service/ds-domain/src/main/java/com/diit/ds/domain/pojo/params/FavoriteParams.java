package com.diit.ds.domain.pojo.params;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * @author yjxbz
 */
@Data
@Schema(description = "添加、取消收藏参数")
@AllArgsConstructor
@NoArgsConstructor
public class FavoriteParams {

    @Schema(description = "智能体id")
    private Integer agentId;

    @Schema(description = "用户id")
    private String userId;

    @Schema(description = "true 收藏 false 取消收藏")
    private Boolean ifAdd;
}
