package com.diit.ds.agent.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.diit.ds.domain.mapper.AgentsTagMapper;
import com.diit.ds.domain.pojo.entity.AgentsTag;
import com.diit.ds.agent.service.AgentsTagService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * @author yjxbz
 */
@Slf4j
@RequiredArgsConstructor
@Service
public class AgentsTagServiceImpl extends ServiceImpl<AgentsTagMapper, AgentsTag> implements AgentsTagService {

    @Override
    public List<AgentsTag> getTags() {
        LambdaQueryWrapper<AgentsTag> wrapper = new LambdaQueryWrapper<>();
        wrapper.orderByAsc(AgentsTag::getOrders);
        return this.baseMapper.selectList(wrapper);
    }
}
