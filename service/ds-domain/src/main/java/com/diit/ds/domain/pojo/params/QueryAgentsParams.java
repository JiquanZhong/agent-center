package com.diit.ds.domain.pojo.params;

import com.diit.ds.domain.pojo.enums.OrderConditionEnums;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

/**
 * @author yjxbz
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "查询智能体列表参数")
public class QueryAgentsParams {

    @Schema(description = "智能体名称")
    private String agentName;

    @Schema(description = "标签")
    private Integer tag;

    @Schema(description = "排序条件,可选类型是" +
            "（CREATE_DATE_ASC： 创建时间升序）" +
            "（CREATE_DATE_DESC： 创建时间降序）" +
            "（FAVORITE_COUNT_ASC：收藏量升序）" +
            "（FAVORITE_COUNT_DESC：收藏量降序）")
    private List<OrderConditionEnums> orderCondition = new ArrayList<>();

    @Schema(description = "当前用户id")
    private String userId;

    @Schema(description = "页码")
    private Integer page = 1;

    @Schema(description = "每页数量")
    private Integer size = 10;
}
