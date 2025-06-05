package com.diit.ds.rag.service.impl;

import com.baomidou.dynamic.datasource.annotation.DS;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.diit.ds.domain.mapper.WorkflowRunMapper;
import com.diit.ds.domain.pojo.entity.WorkflowRun;
import com.diit.ds.rag.service.WorkflowRunService;
import org.springframework.stereotype.Service;

/**
* @author test
* @description 针对表【workflow_run(RAG工作流运行主表)】的数据库操作Service实现
* @createDate 2025-04-05 12:32:20
*/
@Service
@DS("primary")
public class WorkflowRunServiceImpl extends ServiceImpl<WorkflowRunMapper, WorkflowRun>
    implements WorkflowRunService{

}




