package com.diit.ds.chat.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.IService;
import com.diit.ds.domain.pojo.dto.ConversationDTO;
import com.diit.ds.domain.pojo.entity.dify.Conversation;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * 会话服务接口
 */
public interface ConversationService extends IService<Conversation> {
    
    /**
     * 分页查询会话列表
     *
     * @param page 页码
     * @param limit 每页数量
     * @param appId 应用ID
     * @param start 开始时间
     * @param end 结束时间
     * @param sortBy 排序方式
     * @param annotationStatus 标注状态
     * @return 分页会话列表
     */
    IPage<ConversationDTO> getConversationPage(
            Integer page,
            Integer limit,
            UUID appId,
            LocalDateTime start,
            LocalDateTime end,
            String sortBy,
            String annotationStatus,
            String systemName,
            String userName,
            String dialogTitle
    );
} 