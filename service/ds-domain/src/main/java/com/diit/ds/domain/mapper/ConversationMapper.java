package com.diit.ds.domain.mapper;

import com.baomidou.dynamic.datasource.annotation.DS;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.diit.ds.domain.pojo.dto.ConversationDTO;
import com.diit.ds.domain.pojo.entity.dify.Conversation;
import org.apache.ibatis.annotations.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * 会话Mapper接口
 */
@DS("dify")
@Repository
public interface ConversationMapper extends BaseMapper<Conversation> {
    
    /**
     * 分页查询会话列表
     *
     * @param page 分页参数
     * @param appId 应用ID
     * @param start 开始时间
     * @param end 结束时间
     * @param sortBy 排序方式
     * @param annotationStatus 标注状态
     * @param systemName 系统名称
     * @param userName 用户名称
     * @param dialogTitle 对话标题
     * @return 分页会话列表
     */
    IPage<ConversationDTO> selectConversationPage(
            Page<ConversationDTO> page,
            @Param("appId") UUID appId,
            @Param("start") LocalDateTime start,
            @Param("end") LocalDateTime end,
            @Param("sortBy") String sortBy,
            @Param("annotationStatus") String annotationStatus,
            @Param("systemName") String systemName,
            @Param("userName") String userName,
            @Param("dialogTitle") String dialogTitle
    );
} 