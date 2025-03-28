package com.diit.ds.domain.req;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Dify鉴权接口的请求DTO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DifyAuthReq {

    /*
     * 固定值为：email
     */
    @JsonProperty("email")
    private String email;

    /**
     * 应用的 password
     */
    @JsonProperty("password")
    private String password;

    /**
     * 应用的 APP_SECRET
     */
    @JsonProperty("language")
    private String language;

    /**
     * 应用的 remember_me
     */
    @JsonProperty("remember_me")
    private String rememberMe;
} 