package com.diit.ds.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.diit.ds.pojo.entity.AgentsFavorite;

import java.util.List;

/**
 * @author yjxbz
 */
public interface AgentsFavoriteService extends IService<AgentsFavorite> {

    List<AgentsFavorite> queryListByUserId(String userId);

    Integer addFavorite(Integer agentId, String userId);

    Integer cancelFavorite(Integer agentId, String userId);
}
