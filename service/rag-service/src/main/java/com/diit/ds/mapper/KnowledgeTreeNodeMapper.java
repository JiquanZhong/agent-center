package com.diit.ds.mapper;

import com.baomidou.dynamic.datasource.annotation.DS;
import com.diit.ds.domain.entity.KnowledgeTreeNode;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.springframework.stereotype.Repository;

/**
* @author test
* @description 针对表【knowledge_tree_node(知识中心 树节点表)】的数据库操作Mapper
* @createDate 2025-03-31 13:11:23
* @Entity com.diit.ds.domain.entity.KnowledgeTreeNode
*/
@DS("primary")
@Repository
public interface KnowledgeTreeNodeMapper extends BaseMapper<KnowledgeTreeNode> {

}




