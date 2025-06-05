package com.diit.ds.domain.mapper;

import com.baomidou.dynamic.datasource.annotation.DS;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.diit.ds.domain.pojo.entity.Knowledgebase;
import org.springframework.stereotype.Repository;

/**
* @author test
* @description 针对表【knowledgebase】的数据库操作Mapper
* @createDate 2025-05-09 17:32:19
* @Entity com.diit.ds.domain.entity.Knowledgebase
*/
@DS("ragflow")
@Repository
public interface KnowledgebaseMapper extends BaseMapper<Knowledgebase> {

}




