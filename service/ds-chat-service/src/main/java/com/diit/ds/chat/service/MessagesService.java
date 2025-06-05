package com.diit.ds.chat.service;

import com.baomidou.dynamic.datasource.annotation.DS;
import com.baomidou.mybatisplus.extension.service.IService;
import com.diit.ds.domain.pojo.entity.dify.Messages;

import java.util.List;
import java.util.Map;

/**
* @author test
* @description 针对表【messages】的数据库操作Service
* @createDate 2025-04-06 17:43:49
*/
@DS("dify")
public interface MessagesService extends IService<Messages> {
    
    /**
     * 根据消息ID列表批量获取workflowRunId
     * @param messageIds 消息ID列表
     * @return Map<messageId, workflowRunId>
     */
    Map<String, Object> getWorkflowRunIdsByMessageIds(List<String> messageIds);
}
