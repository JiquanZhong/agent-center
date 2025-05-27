package com.diit.ds.transform;

import com.diit.ds.pojo.entity.Agents;
import com.diit.ds.pojo.vo.AgentsVo;
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
