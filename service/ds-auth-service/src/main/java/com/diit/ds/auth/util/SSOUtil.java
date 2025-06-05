package com.diit.ds.auth.util;

import com.diit.ds.common.context.UserContext;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.util.Map;

/**
 * 单点登录工具类，用于处理与外部系统的单点登录交互
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class SSOUtil {

    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;

    @Value("${diit.sso.external-system-validation-url:http://192.168.164.45:8021/datahub/usercenter/userinfo/validation}")
    private String externalSystemValidationUrl;

    /**
     * 验证外部系统的令牌
     *
     * @param token 外部系统的令牌
     * @return 验证结果，成功返回true，失败返回false
     */
    public boolean validateExternalToken(String token) {
        try {
            log.debug("验证外部系统令牌: {}", token);

            // 调用外部系统的验证接口
            HttpHeaders headers = new HttpHeaders();
            headers.set("Content-Type", "application/json");

            HttpEntity<String> entity = new HttpEntity<>(headers);

            String validationUrl = externalSystemValidationUrl + "/" + token;
            log.info("请求验证URL: {}", validationUrl);

            ResponseEntity<String> response = restTemplate.exchange(
                    validationUrl,
                    HttpMethod.GET,
                    entity,
                    String.class
            );

            if (response.getStatusCode().is2xxSuccessful()) {
                Map<String, Object> responseMap = objectMapper.readValue(response.getBody(), Map.class);
                log.debug("验证响应: {}", responseMap);

                // 根据外部系统的响应格式进行解析
                if (responseMap.containsKey("data") && responseMap.get("statusCode").equals(200)) {
                    Map<String, Object> userData = (Map<String, Object>) responseMap.get("data");

                    // 将用户信息存储到UserContext中
                    if (userData.containsKey("id")) {
                        UserContext.setUserId(userData.get("id").toString());
                    }

                    if (userData.containsKey("trueName")) {
                        UserContext.setUserName(userData.get("trueName").toString());
                    }

                    if (userData.containsKey("id")) {
                        UserContext.setLoginName(userData.get("trueName").toString());
                    }

                    UserContext.setUserRoles("普通用户");

                    log.info("成功验证外部系统令牌并设置用户上下文，用户ID: {}, 用户名: {}, 登录名: {}, 用户角色: {}",
                            UserContext.getUserId(), UserContext.getUserName(), UserContext.getLoginName(), UserContext.getUserRoles());

                    return true;
                }
            }

            log.warn("验证外部系统令牌失败，响应状态码: {}, 响应体：{}", response.getStatusCode(), response.getBody());
            return false;
        } catch (Exception e) {
            log.error("验证外部系统令牌异常", e);
            return false;
        }
    }
}
