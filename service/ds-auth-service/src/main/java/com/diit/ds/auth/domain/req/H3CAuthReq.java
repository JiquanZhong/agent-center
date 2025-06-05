package com.diit.ds.auth.domain.req;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * H3C鉴权接口的请求DTO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class H3CAuthReq {

    /*
     * 固定值为：client_credentials
     */
    @JsonProperty("grant_type")
    private String grantType;

    /**
     * 应用的 APP_KEY
     */
    @JsonProperty("client_id")
    private String clientId;

    /**
     * 应用的 APP_SECRET
     */
    @JsonProperty("client_secret")
    private String clientSecret;
} 