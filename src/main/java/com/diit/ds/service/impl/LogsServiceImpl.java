package com.diit.ds.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.diit.ds.domain.entity.Logs;
import com.diit.ds.service.LogsService;
import com.diit.ds.mapper.LogsMapper;
import org.springframework.stereotype.Service;

/**
* @author test
* @description 针对表【htyy_logs(日志表)】的数据库操作Service实现
* @createDate 2025-03-03 14:21:22
*/
@Service
public class LogsServiceImpl extends ServiceImpl<LogsMapper, Logs>
    implements LogsService{

}




