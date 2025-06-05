package com.diit.ds.agent.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.diit.ds.domain.pojo.entity.AgentsTag;

import java.util.List;

/**
 * @author yjxbz
 */
public interface AgentsTagService extends IService<AgentsTag> {
    List<AgentsTag> getTags();

}
