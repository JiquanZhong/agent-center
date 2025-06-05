package com.diit.ds.domain.pojo.params;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * @author yjxbz
 */
@Data
@Schema(description = "获取用户收藏列表参数")
@AllArgsConstructor
@NoArgsConstructor
public class GetUserFavoriteParams {

    @Schema(description = "用户ID")
    private String userId;

    @Schema(description = "页码")
    private Integer page;

    @Schema(description = "每页数量")
    private Integer size;
}
