package com.diit.ds.service;

import com.baomidou.dynamic.datasource.annotation.DS;
import com.diit.ds.domain.entity.Document;
import com.baomidou.mybatisplus.extension.service.IService;

/**
* @author test
* @description 针对表【document】的数据库操作Service
* @createDate 2025-04-01 17:20:03
*/
@DS("ragflow")
public interface DocumentService extends IService<Document> {

    String getDatasetId(String documentId);
    
    /**
     * 根据知识库ID获取文档数量
     * @param kbId 知识库ID
     * @return 文档数量
     */
    Integer countDocumentsByKbId(String kbId);

    /**
     * 根据知识库ID获取文档大小
     * @param kbId 知识库ID
     * @return 文档大小
     */
    Long countDocumentSizeByKbId(String kbId);

    /**
     * 根据知识库ID获取词元数量
     * @param kbId 知识库ID
     * @return 词元数量
     */
    Long countTokensByKbId(String kbId);

    /**
     * 根据知识库ID获取文本块数量
     * @param kbId 知识库ID
     * @return 文本块数量
     */
    Integer countChunksByKbId(String kbId);
}
