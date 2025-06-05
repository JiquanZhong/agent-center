package com.diit.ds.rag.structmapper;

import com.diit.ds.rag.domain.dto.KnowledgeTreeNodeDTO;
import com.diit.ds.rag.domain.dto.KnowledgeTreeStatisticDTO;
import com.diit.ds.rag.domain.req.KnowledgeTreeNodeCreateReq;
import com.diit.ds.rag.domain.req.KnowledgeTreeNodeUpdateReq;
import com.diit.ds.domain.pojo.entity.KnowledgeTreeNode;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.factory.Mappers;

@Mapper
public interface KnowledgeTreeNodeSM {

    KnowledgeTreeNodeSM INSTANCE = Mappers.getMapper(KnowledgeTreeNodeSM.class);

    @Mapping(source = "kdbId", target = "kdbId")
    @Mapping(source = "sortOrder", target = "sortOrder")
    KnowledgeTreeNodeDTO entity2DTO(KnowledgeTreeNode node);

    KnowledgeTreeStatisticDTO entity2StatisticDTO(KnowledgeTreeNode node);


    KnowledgeTreeNode createDTO2Entity(KnowledgeTreeNodeCreateReq dto);

    KnowledgeTreeNode updateDTO2Entity(KnowledgeTreeNodeUpdateReq dto);
}
