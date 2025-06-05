package com.diit.ds.agent.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.conditions.query.LambdaQueryChainWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.diit.ds.domain.mapper.AgentsFavoriteMapper;
import com.diit.ds.domain.pojo.entity.AgentsFavorite;
import com.diit.ds.agent.service.AgentsFavoriteService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * @author yjxbz
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AgentsFavoriteServiceImpl extends ServiceImpl<AgentsFavoriteMapper, AgentsFavorite> implements AgentsFavoriteService {
    @Override
    public List<AgentsFavorite> queryListByUserId(String userId) {
        return new LambdaQueryChainWrapper<>(baseMapper)
                .eq(AgentsFavorite::getUserId, userId).list();
    }

    @Override
    public Integer addFavorite(Integer agentId, String userId) {
        // 判断当前用户是否已经收藏了这个智能体
        LambdaQueryWrapper<AgentsFavorite> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(AgentsFavorite::getAgentId, agentId);
        wrapper.eq(AgentsFavorite::getUserId, userId);
        if (this.count(wrapper) > 0) {
            // 已经收藏了，则不添加
            log.info("当前用户已经收藏过该智能体不能重复添加,UserId:{},AgentId:{}", userId, agentId);
            return -1;
        }

        // 添加收藏
        AgentsFavorite agentsFavorite = new AgentsFavorite();
        agentsFavorite.setAgentId(agentId);
        agentsFavorite.setUserId(userId);
        this.save(agentsFavorite);
        log.info("添加智能体收藏成功,UserId:{},AgentId:{}", userId, agentId);
        return 1;
    }

    @Override
    public Integer cancelFavorite(Integer agentId, String userId) {
        // 判断当前用户是否已经收藏了这个智能体
        LambdaQueryWrapper<AgentsFavorite> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(AgentsFavorite::getAgentId, agentId);
        wrapper.eq(AgentsFavorite::getUserId, userId);
        if (this.count(wrapper) == 0) {
            // 未收藏过
            log.info("当前用户未收藏过该智能体不能取消,UserId:{},AgentId:{}", userId, agentId);
            return -1;
        }

        this.baseMapper.delete(wrapper);
        return 1;
    }
}
