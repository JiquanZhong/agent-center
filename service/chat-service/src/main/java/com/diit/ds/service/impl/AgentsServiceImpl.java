package com.diit.ds.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.diit.ds.domain.entity.Agents;
import com.diit.ds.service.AgentsService;
import com.diit.ds.mapper.AgentsMapper;
import org.springframework.stereotype.Service;

/**
* @author test
* @description 针对表【agents(智能体表)】的数据库操作Service实现
* @createDate 2025-05-29 16:11:52
*/
@Service
public class AgentsServiceImpl extends ServiceImpl<AgentsMapper, Agents>
    implements AgentsService{

}




