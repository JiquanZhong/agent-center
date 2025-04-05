package com.diit.ds.structmapper;

import com.diit.ds.domain.dto.WorkflowDocAggDTO;
import com.diit.ds.domain.entity.WorkflowDocAgg;
import org.mapstruct.Mapper;
import org.mapstruct.factory.Mappers;

@Mapper
public interface WorkflowDocAggSM {
    WorkflowDocAggSM INSTANCE = Mappers.getMapper(WorkflowDocAggSM.class);

    WorkflowDocAggDTO toDTO(WorkflowDocAgg workflowDocAgg);
}
