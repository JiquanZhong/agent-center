package com.diit.ds.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.diit.ds.domain.entity.Favorites;
import com.diit.ds.service.FavoritesService;
import com.diit.ds.mapper.FavoritesMapper;
import org.springframework.stereotype.Service;

/**
* @author test
* @description 针对表【htyy_favorites(收藏表)】的数据库操作Service实现
* @createDate 2025-03-03 14:21:22
*/
@Service
public class FavoritesServiceImpl extends ServiceImpl<FavoritesMapper, Favorites>
    implements FavoritesService{

}




