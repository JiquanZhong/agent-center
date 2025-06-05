package com.diit.ds.rag.structmapper;

import com.diit.ds.rag.domain.dto.WorkflowChunkDTO;
import com.diit.ds.domain.pojo.entity.WorkflowChunk;
import org.mapstruct.Mapper;
import org.mapstruct.factory.Mappers;

@Mapper
public interface WorkflowChunkSM {
    WorkflowChunkSM INSTANCE = Mappers.getMapper(WorkflowChunkSM.class);

    WorkflowChunkDTO toDTO(WorkflowChunk workflowChunk);
}
