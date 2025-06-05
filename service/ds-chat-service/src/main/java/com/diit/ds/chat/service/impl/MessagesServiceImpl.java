package com.diit.ds.chat.service.impl;

import com.baomidou.dynamic.datasource.annotation.DS;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.diit.ds.domain.pojo.entity.dify.Messages;
import com.diit.ds.chat.service.MessagesService;
import com.diit.ds.domain.mapper.MessagesMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

/**
* @author test
* @description 针对表【messages】的数据库操作Service实现
* @createDate 2025-04-06 17:43:49
*/
@Slf4j
@Service
@DS("dify")
public class MessagesServiceImpl extends ServiceImpl<MessagesMapper, Messages>
    implements MessagesService{

    @Override
    public Map<String, Object> getWorkflowRunIdsByMessageIds(List<String> messageIds) {
        if (messageIds == null || messageIds.isEmpty()) {
            return new HashMap<>();
        }
        
        try {
            // 将字符串ID转换为UUID
            List<UUID> uuidIds = messageIds.stream()
                    .map(UUID::fromString)
                    .collect(Collectors.toList());
            
            // 查询指定ID的消息记录
            LambdaQueryWrapper<Messages> queryWrapper = new LambdaQueryWrapper<>();
            queryWrapper.in(Messages::getId, uuidIds);
            queryWrapper.select(Messages::getId, Messages::getWorkflowRunId);
            
            List<Messages> messages = this.list(queryWrapper);
            
            // 转换为Map<messageId, workflowRunId>
            return messages.stream()
                    .collect(Collectors.toMap(
                            message -> message.getId().toString(),
                            Messages::getWorkflowRunId,
                            (oldValue, newValue) -> newValue
                    ));
        } catch (Exception e) {
            // 日志记录错误
            log.error("获取workflowRunIds失败", e);
            return new HashMap<>();
        }
    }
}




