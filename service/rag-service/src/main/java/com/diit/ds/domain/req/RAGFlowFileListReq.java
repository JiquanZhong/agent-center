package com.diit.ds.domain.req;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * RAGFlow文件列表查询请求
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RAGFlowFileListReq {
    /**
     * 当前页码
     */
    private Integer page;
    
    /**
     * 每页数量
     */
    @JsonProperty("page_size")
    private Integer pageSize;
    
    /**
     * 排序字段
     */
    private String orderby;
    
    /**
     * 是否降序
     */
    private Boolean desc;
    
    /**
     * 关键词
     */
    private String keywords;
    
    /**
     * 文档ID
     */
    private String id;
    
    /**
     * 文档名称
     */
    private String name;
    
    /**
     * 文档状态
     * 可选值如下：
     * 未解析 - 文件已上传但未开始解析
     * 解析中 - 文件正在解析中
     * 已入库 - 文件已解析完成并入库
     * 已取消 - 文件解析被取消
     * 解析失败 - 文件解析失败
     */
    private String status;
} 