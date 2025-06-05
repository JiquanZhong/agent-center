package com.diit.ds.agent.transform;

import com.diit.ds.domain.pojo.entity.Agents;
import com.diit.ds.domain.pojo.vo.AgentsVo;
import org.mapstruct.Mapper;
import org.mapstruct.factory.Mappers;

/**
 * @author yjxbz
 */
@Mapper
public interface AgentsTransform {
    AgentsTransform INSTANCE = Mappers.getMapper(AgentsTransform.class);

    AgentsVo toVo(Agents agents);
}
