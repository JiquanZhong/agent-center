package com.diit.ds.rag.structmapper;

import com.diit.ds.rag.domain.resp.DifyKnowledgeResp;
import org.mapstruct.Mapper;
import org.mapstruct.factory.Mappers;

@Mapper
public interface DifyKnowledgeRespSM {
    DifyKnowledgeRespSM INSTANCE = Mappers.getMapper(DifyKnowledgeRespSM.class);

    DifyKnowledgeResp.SimpleRecord toSimpleRecord(DifyKnowledgeResp.Record record);
}
