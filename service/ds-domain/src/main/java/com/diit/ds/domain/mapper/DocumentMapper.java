package com.diit.ds.domain.mapper;

import com.baomidou.dynamic.datasource.annotation.DS;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.diit.ds.domain.pojo.entity.Document;
import org.springframework.stereotype.Repository;

/**
* @author test
* @description 针对表【document】的数据库操作Mapper
* @createDate 2025-04-01 17:20:03
* @Entity com.diit.ds.domain.entity.Document
*/
@DS("ragflow")
@Repository
public interface DocumentMapper extends BaseMapper<Document> {

}




