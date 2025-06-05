package com.diit.ds.rag.service;

import com.baomidou.dynamic.datasource.annotation.DS;
import com.diit.ds.domain.pojo.entity.Knowledgebase;
import com.baomidou.mybatisplus.extension.service.IService;

/**
* @author test
* @description 针对表【knowledgebase】的数据库操作Service
* @createDate 2025-05-09 17:32:19
*/
@DS("ragflow")
public interface KnowledgebaseService extends IService<Knowledgebase> {

}
