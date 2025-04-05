package com.diit.ds.mapper;

import com.baomidou.dynamic.datasource.annotation.DS;
import com.diit.ds.domain.entity.WorkflowRun;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;

/**
* @author test
* @description 针对表【workflow_run(RAG工作流运行主表)】的数据库操作Mapper
* @createDate 2025-04-05 12:32:20
* @Entity com.diit.ds.domain.entity.WorkflowRun
*/
@DS("primary")
public interface WorkflowRunMapper extends BaseMapper<WorkflowRun> {

}




