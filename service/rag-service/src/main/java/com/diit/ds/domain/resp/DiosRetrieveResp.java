package com.diit.ds.domain.resp;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import java.util.List;

/**
 * Dios检索响应类
 */
@Data
public class DiosRetrieveResp {
    private List<RetrieveResult> retrieveResults;
    
    @Data
    public static class RetrieveResult {
        /**
         * 文件名
         */
        private String filename;
        
        /**
         * 资源ID
         */
        @JsonProperty("resource_id")
        private Integer resourceId;
        
        /**
         * 文本内容
         */
        private String text;
        
        /**
         * 相似度得分
         */
        private Double score;
    }
} 