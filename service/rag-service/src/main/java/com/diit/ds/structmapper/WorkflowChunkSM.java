package com.diit.ds.structmapper;

import com.diit.ds.domain.dto.WorkflowChunkDTO;
import com.diit.ds.domain.entity.WorkflowChunk;
import org.mapstruct.Mapper;
import org.mapstruct.factory.Mappers;

@Mapper
public interface WorkflowChunkSM {
    WorkflowChunkSM INSTANCE = Mappers.getMapper(WorkflowChunkSM.class);

    WorkflowChunkDTO toDTO(WorkflowChunk workflowChunk);
}
