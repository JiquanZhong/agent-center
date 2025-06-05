package com.diit.ds.domain.pojo.params;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;


/**
 * @author yjxbz
 */
/**
 * @author yjxbz
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
@Schema(description = "获取当前用户的智能体列表")
public class GetUserAgentsParams{

    @Schema(description = "用户ID")
    private String userId;

    @Schema(description = "页码")
    private Integer page;

    @Schema(description = "每页数量")
    private Integer size;

}