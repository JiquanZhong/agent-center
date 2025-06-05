package com.diit.ds.rag.structmapper;

import com.diit.ds.rag.domain.dto.WorkflowDocAggDTO;
import com.diit.ds.domain.pojo.entity.WorkflowDocAgg;
import org.mapstruct.Mapper;
import org.mapstruct.factory.Mappers;

@Mapper
public interface WorkflowDocAggSM {
    WorkflowDocAggSM INSTANCE = Mappers.getMapper(WorkflowDocAggSM.class);

    WorkflowDocAggDTO toDTO(WorkflowDocAgg workflowDocAgg);
}
