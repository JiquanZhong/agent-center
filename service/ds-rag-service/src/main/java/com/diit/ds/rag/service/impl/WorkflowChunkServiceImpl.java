package com.diit.ds.rag.service.impl;

import com.baomidou.dynamic.datasource.annotation.DS;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.diit.ds.domain.mapper.WorkflowChunkMapper;
import com.diit.ds.domain.pojo.entity.WorkflowChunk;
import com.diit.ds.rag.service.WorkflowChunkService;
import org.springframework.stereotype.Service;

/**
* @author test
* @description 针对表【workflow_chunk(RAG工作流文档片段表)】的数据库操作Service实现
* @createDate 2025-04-05 12:32:20
*/
@Service
@DS("primary")
public class WorkflowChunkServiceImpl extends ServiceImpl<WorkflowChunkMapper, WorkflowChunk>
    implements WorkflowChunkService{

}




