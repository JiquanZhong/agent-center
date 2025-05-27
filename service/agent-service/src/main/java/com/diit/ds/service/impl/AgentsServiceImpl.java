package com.diit.ds.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.diit.ds.pojo.entity.Agents;
import com.diit.ds.mapper.AgentsMapper;
import com.diit.ds.pojo.entity.AgentsFavorite;
import com.diit.ds.pojo.enums.AgentStatus;
import com.diit.ds.pojo.enums.OrderConditionEnums;
import com.diit.ds.pojo.params.*;
import com.diit.ds.pojo.vo.AgentsVo;
import com.diit.ds.service.AgentsFavoriteService;
import com.diit.ds.service.AgentsService;
import com.diit.ds.service.AgentsTagService;
import com.diit.ds.transform.AgentsTransform;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import static com.diit.ds.conts.Constraint.SORT_FIELD_MAP;

/**
 * @author yjxbz
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AgentsServiceImpl extends ServiceImpl<AgentsMapper, Agents> implements AgentsService {

    private final AgentsFavoriteService agentsFavoriteService;

    private final AgentsTagService agentsTagService;

    @Override
    public Page<AgentsVo> getAgentsList(QueryAgentsParams params) {
        log.info("当前智能体列表查询参数为:{}", params);
        // 创建MyBatis-Plus的Page对象
        Page<Agents> page = new Page<>(params.getPage(), params.getSize());

        LambdaQueryWrapper<Agents> wrapper = new LambdaQueryWrapper<>();
        // 广场查询
        // 如果名字不为空
        if (StringUtils.hasText(params.getAgentName())) {
            wrapper.like(Agents::getName, params.getAgentName());
        }
        // 如果标签不为空的话
        if (params.getTag() != null && params.getTag()!=0) {
            wrapper.eq(Agents::getTag, params.getTag());
        }
        // 智能体必须是发布状态
        wrapper.eq(Agents::getStatus, AgentStatus.PUBLISHED);
        // 处理排序条件
        if (params.getOrderCondition() != null && !params.getOrderCondition().isEmpty()) {
            for (OrderConditionEnums orderCondition : params.getOrderCondition()) {
                if (orderCondition != null && SORT_FIELD_MAP.containsKey(orderCondition.getValue())) {
                    wrapper.orderBy(true, orderCondition.isCode(), SORT_FIELD_MAP.get(orderCondition.getValue()));
                }
            }
        } else {
            // 默认按创建时间降序
            wrapper.orderByDesc(Agents::getCreateDate);
        }

        try {
            // 使用MyBatis-Plus的page方法进行分页查询
            Page<Agents> result = this.page(page, wrapper);
            return agents2Vo(result,params.getUserId());
        } catch (Exception e) {
            log.error("获取广场智能体列表失败", e);
            // 返回空记录的Page对象
            Page<AgentsVo> emptyPage = new Page<>(params.getPage(), params.getSize());
            emptyPage.setRecords(new ArrayList<>());
            emptyPage.setTotal(0);
            return emptyPage;
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean favorite(FavoriteParams params) {
        Integer i = 0;
        String condition = "";
        if (params.getIfAdd()){
            // 添加收藏
            i = agentsFavoriteService.addFavorite(params.getAgentId(), params.getUserId());
            condition = "favorite_count = favorite_count + 1";
        }
        else {
            // 取消收藏
            i = agentsFavoriteService.cancelFavorite(params.getAgentId(), params.getUserId());
            condition = "favorite_count = favorite_count - 1";
        }
        if (i > 0){
            // 更新智能体收藏数,这里需要加锁
            LambdaUpdateWrapper<Agents> wrapper = new LambdaUpdateWrapper<>();
            wrapper.eq(Agents::getId, params.getAgentId())
                    .setSql(condition);
            // 这里需要加锁
            synchronized (this){
                this.update(wrapper);
            }
            return true;
        }
        return false;
    }

    @Override
    public Page<AgentsVo> getUserFavorite(GetUserFavoriteParams params) {
        try {
            // 是个人收藏的智能体查询
            // 先查询当前用户收藏了哪些Agents
            List<AgentsFavorite> agentsFavorites = agentsFavoriteService.queryListByUserId(params.getUserId());

            if (agentsFavorites != null && !agentsFavorites.isEmpty()) {
                List<Integer> agentIds = agentsFavorites
                        .stream()
                        .map(AgentsFavorite::getAgentId)
                        .collect(Collectors.toList());

                // 根据agentIds查询Agents
                List<Agents> agents = this.listByIds(agentIds);
                // 手动设置Page对象的记录和总数
                Page<Agents> resultPage = new Page<>(params.getPage(), params.getSize());
                resultPage.setRecords(agents);
                resultPage.setTotal(agents.size());
                return agents2Vo(resultPage,params.getUserId());
            } else {
                // 返回空记录的Page对象
                Page<AgentsVo> emptyPage = new Page<>(params.getPage(), params.getSize());
                emptyPage.setRecords(new ArrayList<>());
                emptyPage.setTotal(0);
                return emptyPage;
            }
        } catch (Exception e) {
            log.error("获取用户收藏智能体列表失败", e);
            // 返回空记录的Page对象
            Page<AgentsVo> emptyPage = new Page<>(params.getPage(), params.getSize());
            emptyPage.setRecords(new ArrayList<>());
            emptyPage.setTotal(0);
            return emptyPage;
        }
    }

    @Override
    public List<AgentsVo> getHotAgents() {

        LambdaQueryWrapper<Agents> wrapper = new LambdaQueryWrapper<Agents>()
                .orderBy(true, false, Agents::getFavoriteCount)
                .last("LIMIT 10");
        List<Agents> agents = this.baseMapper.selectList(wrapper);
        return agents.stream().map(agent ->
                {
                    AgentsVo agentsVo = AgentsTransform.INSTANCE.toVo(agent);
                    agentsVo.setAgentTag(agentsTagService.getById(agent.getTag()));
                    return agentsVo;
                })
                .collect(Collectors.toList());
    }

    @Override
    public boolean browse(BrowseParams params) {
        return false;
    }

    @Override
    public Page<AgentsVo> getUserAgents(GetUserAgentsParams params) {
        // 创建MyBatis-Plus的Page对象
        Page<Agents> page = new Page<>(params.getPage(), params.getSize());
        // 根据用户ID查询用户创建的智能体列表
        LambdaQueryWrapper<Agents> wrapper = new LambdaQueryWrapper<Agents>()
                .eq(Agents::getCreateUser, params.getUserId())
                .orderByDesc(Agents::getCreateDate);
        Page<Agents> agentsPage = this.baseMapper.selectPage(page, wrapper);
        return agents2Vo(agentsPage,params.getUserId());
    }


    public Page<AgentsVo> agents2Vo(Page<Agents> agentsPage,String userId){
        // 使用Stream API将Agents转换为AgentsVo
        List<AgentsVo> agentsVoList = agentsPage.getRecords()
                .stream()
                .map(agent ->
                {
                    AgentsVo agentsVo = AgentsTransform.INSTANCE.toVo(agent);
                    agentsVo.setAgentTag(agentsTagService.getById(agent.getTag()));

                    // 判断当前智能体是否被收藏
                    LambdaQueryWrapper<AgentsFavorite> wrapper = new LambdaQueryWrapper<>();
                    wrapper.eq(AgentsFavorite::getAgentId, agent.getId());
                    wrapper.eq(AgentsFavorite::getUserId, userId);
                    long count = agentsFavoriteService.count(wrapper);
                    agentsVo.setIfFavorite(count > 0);

                    return agentsVo;
                })
                .collect(Collectors.toList());

        // 创建新的Page对象返回
        Page<AgentsVo> agentsVoPage = new Page<>(agentsPage.getCurrent(), agentsPage.getSize());
        agentsVoPage.setRecords(agentsVoList);
        agentsVoPage.setTotal(agentsPage.getTotal());
        return agentsVoPage;
    }
}
