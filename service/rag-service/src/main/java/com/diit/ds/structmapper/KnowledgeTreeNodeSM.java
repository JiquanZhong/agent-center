package com.diit.ds.structmapper;

import com.diit.ds.domain.dto.KnowledgeTreeNodeDTO;
import com.diit.ds.domain.entity.KnowledgeTreeNode;
import com.diit.ds.domain.req.KnowledgeTreeNodeCreateReq;
import com.diit.ds.domain.req.KnowledgeTreeNodeUpdateReq;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.factory.Mappers;

@Mapper
public interface KnowledgeTreeNodeSM {

    KnowledgeTreeNodeSM INSTANCE = Mappers.getMapper(KnowledgeTreeNodeSM.class);

    @Mapping(source = "kdbId", target = "kdbId")
    @Mapping(source = "sortOrder", target = "sortOrder")
    KnowledgeTreeNodeDTO entity2DTO(KnowledgeTreeNode node);

    KnowledgeTreeNode createDTO2Entity(KnowledgeTreeNodeCreateReq dto);

    KnowledgeTreeNode updateDTO2Entity(KnowledgeTreeNodeUpdateReq dto);
}
