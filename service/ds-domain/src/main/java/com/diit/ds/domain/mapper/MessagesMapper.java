package com.diit.ds.domain.mapper;

import com.baomidou.dynamic.datasource.annotation.DS;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.diit.ds.domain.pojo.entity.dify.Messages;

/**
* @author test
* @description 针对表【messages】的数据库操作Mapper
* @createDate 2025-04-06 17:43:49
* @Entity com.diit.ds.domain.entity.dify.Messages
*/
@DS("dify")
public interface MessagesMapper extends BaseMapper<Messages> {

}




