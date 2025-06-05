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
public class H3CAuthResp {
    /**
     * 响应状态码
     */
    @JsonProperty("code")
    private Integer code;

    /**
     * 响应消息
     */
    @JsonProperty("msg")
    private String msg;

    /**
     * 响应数据
     */
    @JsonProperty("data")
    private AuthData data;

    /**
     * 返回的接口调用凭证
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class AuthData {
        /**
         * 接口调用凭证
         */
        @JsonProperty("access_token")
        private String accessToken;

        /**
         * token 过期时间
         */
        @JsonProperty("expire_time")
        private Date expireTime;

    }
} 