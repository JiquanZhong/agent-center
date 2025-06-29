package com.diit.ds.agent.conts;

import com.baomidou.mybatisplus.core.toolkit.support.SFunction;
import com.diit.ds.domain.pojo.entity.Agents;

import java.util.Map;

public class Constraint {

    public static final Map<String, SFunction<Agents, ?>> SORT_FIELD_MAP = Map.of(
            "create_date", Agents::getCreateDate,
            "favorite_count",  Agents::getFavoriteCount
    );
}
