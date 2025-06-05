package com.diit.ds.rag.domain.resp;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Date;

/**
 * H3C鉴权接口的响应DTO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DifyAuthResp {
    /**
     * 响应状态码
     */
    @JsonProperty("result")
    private String result;

    /**
     * 响应数据
     */
    @JsonProperty("data")
    private DifyAuthData data;

    /**
     * 返回的接口调用凭证
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class DifyAuthData {
        /**
         * 接口调用凭证
         */
        @JsonProperty("access_token")
        private String accessToken;

        /**
         * refresh token
         */
        @JsonProperty("refresh_token")
        private String refreshToken;

    }
} 