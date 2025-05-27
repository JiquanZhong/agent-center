package com.diit.ds.pojo.params;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * @author yjxbz
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
@Schema(description = "修改浏览量参数")
public class BrowseParams {

    private Integer agentId;
}
