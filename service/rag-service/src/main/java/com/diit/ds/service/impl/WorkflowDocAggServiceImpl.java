package com.diit.ds.service.impl;

import com.baomidou.dynamic.datasource.annotation.DS;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.diit.ds.domain.entity.WorkflowDocAgg;
import com.diit.ds.service.WorkflowDocAggService;
import com.diit.ds.mapper.WorkflowDocAggMapper;
import org.springframework.stereotype.Service;

/**
* @author test
* @description 针对表【workflow_doc_agg(RAG工作流文档聚合表)】的数据库操作Service实现
* @createDate 2025-04-05 12:32:20
*/
@Service
@DS("primary")
public class WorkflowDocAggServiceImpl extends ServiceImpl<WorkflowDocAggMapper, WorkflowDocAgg>
    implements WorkflowDocAggService{

}




