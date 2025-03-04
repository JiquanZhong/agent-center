package com.diit.ds.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.diit.ds.domain.entity.Users;
import com.diit.ds.service.UsersService;
import com.diit.ds.mapper.UsersMapper;
import org.springframework.stereotype.Service;

/**
* @author test
* @description 针对表【htyy_users(用户表)】的数据库操作Service实现
* @createDate 2025-03-03 14:21:22
*/
@Service
public class UsersServiceImpl extends ServiceImpl<UsersMapper, Users>
    implements UsersService{

}




