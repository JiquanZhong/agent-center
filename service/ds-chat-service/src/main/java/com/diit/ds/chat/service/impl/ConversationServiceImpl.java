package com.diit.ds.chat.service.impl;

import com.baomidou.dynamic.datasource.annotation.DS;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.diit.ds.domain.mapper.ConversationMapper;
import com.diit.ds.domain.pojo.dto.ConversationDTO;
import com.diit.ds.domain.pojo.entity.dify.Conversation;
import com.diit.ds.chat.service.ConversationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * 会话服务实现类
 */
@DS("dify")
@Slf4j
@Service
@RequiredArgsConstructor
public class ConversationServiceImpl extends ServiceImpl<ConversationMapper, Conversation> implements ConversationService {

    private final ConversationMapper conversationMapper;

    /**
     * 分页查询会话列表
     *
     * @param page             页码
     * @param limit            每页数量
     * @param appId            应用ID
     * @param start            开始时间
     * @param end              结束时间
     * @param sortBy           排序方式
     * @param annotationStatus 标注状态
     * @return 分页会话列表
     */
    @Override
    public IPage<ConversationDTO> getConversationPage(Integer page, Integer limit, UUID appId, LocalDateTime start, LocalDateTime end, String sortBy, String annotationStatus, String systemName, String userName, String dialogTitle) {

        Page<ConversationDTO> pageParam = new Page<>(page, limit);

        try {
            // 调用Mapper查询
            return conversationMapper.selectConversationPage(pageParam, appId, start, end, sortBy, annotationStatus, systemName, userName, dialogTitle);
        } catch (Exception e) {
            log.error("分页查询会话异常: {}", e.getMessage(), e);
            return new Page<>();
        }
    }
} 