package com.diit.ds.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.diit.ds.domain.entity.Messages;
import com.diit.ds.service.MessagesService;
import com.diit.ds.mapper.MessagesMapper;
import org.springframework.stereotype.Service;

/**
* @author test
* @description 针对表【htyy_messages(消息表)】的数据库操作Service实现
* @createDate 2025-03-03 14:21:22
*/
@Service
public class MessagesServiceImpl extends ServiceImpl<MessagesMapper, Messages>
    implements MessagesService{

}




