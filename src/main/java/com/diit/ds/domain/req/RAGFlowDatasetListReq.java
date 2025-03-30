package com.diit.ds.domain.req;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

/**
 * RAGFlow列表数据集请求DTO
 * {
 *   "id": "1097d1b80d4c11f0b8bb0242ac150006"
 * }
 */
@Data
@JsonInclude(JsonInclude.Include.NON_NULL)  // 不序列化null值
public class RAGFlowDatasetListReq {
    /**
     * 页码，默认为1
     */
    @JsonProperty("page")
    private Integer page = 1;

    /**
     * 每页数量，默认为30
     */
    @JsonProperty("page_size")
    private Integer pageSize = 30;

    /**
     * 排序字段，可选值：
     * - create_time (默认)
     * - update_time
     */
    @JsonProperty("orderby")
    private String orderby = "create_time";

    /**
     * 是否降序排序，默认为true
     */
    @JsonProperty("desc")
    private Boolean desc = true;

    /**
     * 按数据集名称筛选
     */
    @JsonProperty("name")
    private String name;

    /**
     * 按数据集ID筛选
     */
    @JsonProperty("id")
    private String id;
} 