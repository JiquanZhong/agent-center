package com.diit.ds.service;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import com.diit.ds.pojo.params.*;
import com.diit.ds.pojo.entity.Agents;
import com.diit.ds.pojo.vo.AgentsVo;

import java.util.List;

/** * @author yjxbz */
public interface AgentsService extends IService<Agents> {
    Page<AgentsVo> getAgentsList(QueryAgentsParams params);

    boolean favorite(FavoriteParams params);

    Page<AgentsVo> getUserFavorite(GetUserFavoriteParams params);

    List<AgentsVo> getHotAgents();

    boolean browse(BrowseParams params);

    Page<AgentsVo> getUserAgents(GetUserAgentsParams params);
}
