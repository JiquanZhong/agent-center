package com.diit.ds.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.diit.ds.pojo.entity.AgentsTag;

import java.util.List;

/**
 * @author yjxbz
 */
public interface AgentsTagService extends IService<AgentsTag> {
    List<AgentsTag> getTags();

}
