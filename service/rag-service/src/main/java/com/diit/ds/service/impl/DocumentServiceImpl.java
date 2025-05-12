package com.diit.ds.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.diit.ds.domain.entity.Document;
import com.diit.ds.service.DocumentService;
import com.diit.ds.mapper.DocumentMapper;
import org.springframework.stereotype.Service;

/**
* @author test
* @description 针对表【document】的数据库操作Service实现
* @createDate 2025-04-01 17:20:03
*/
@Service
public class DocumentServiceImpl extends ServiceImpl<DocumentMapper, Document>
    implements DocumentService{

    @Override
    public String getDatasetId(String documentId) {
        return lambdaQuery()
                .eq(Document::getId, documentId)
                .one()
                .getKbId();
    }
    
    @Override
    public Integer countDocumentsByKbId(String kbId) {
        return lambdaQuery()
                .eq(Document::getKbId, kbId)
                .count()
                .intValue();
    }

    @Override
    public Long countDocumentSizeByKbId(String kbId) {
        return lambdaQuery()
                .eq(Document::getKbId, kbId)
                .list()
                .stream()
                .mapToLong(Document::getSize)
                .sum();
    }

    @Override
    public Long countTokensByKbId(String kbId) {
        return lambdaQuery()
                .eq(Document::getKbId, kbId)
                .list()
                .stream()
                .mapToLong(Document::getTokenNum)
                .sum();
    }

    @Override
    public Integer countChunksByKbId(String kbId) {
        return lambdaQuery()
                .eq(Document::getKbId, kbId)
                .list()
                .stream()
                .mapToInt(Document::getChunkNum)
                .sum();
    }
}




