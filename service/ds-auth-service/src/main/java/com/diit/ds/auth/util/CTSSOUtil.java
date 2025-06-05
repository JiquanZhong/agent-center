package com.diit.ds.auth.util;

import com.diit.ds.common.context.UserContext;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.util.List;
import java.util.Map;

/**
 * CT平台单点登录工具类，用于处理与CT平台的单点登录交互
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class CTSSOUtil {

    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;

    @Value("${diit.ctsso.external-system-validation-url}")
    private String validationUrl;

    /**
     * 验证CT平台的令牌
     *
     * @param token CT平台的令牌
     * @return 验证结果，成功返回true，失败返回false
     */
    public boolean validateExternalToken(String token) {
        try {
            log.debug("验证CT平台令牌: {}", token);

            // 构建验证URL
            String checkTokenUrl = validationUrl + "?token=" + token;
            log.info("请求验证URL: {}", checkTokenUrl);

            // 设置请求头
            HttpHeaders headers = new HttpHeaders();
            headers.set("Content-Type", "application/json");

            HttpEntity<String> entity = new HttpEntity<>(headers);

            // 发送请求
            ResponseEntity<String> response = restTemplate.exchange(
                    checkTokenUrl,
                    HttpMethod.GET,
                    entity,
                    String.class
            );

            if (response.getStatusCode().is2xxSuccessful()) {
                Map<String, Object> responseMap = objectMapper.readValue(response.getBody(), Map.class);
                log.debug("验证响应: {}", responseMap);

                // 检查token是否有效
                if (responseMap.containsKey("active") && Boolean.TRUE.equals(responseMap.get("active"))) {
                    // 设置用户信息到上下文
                    setUserContext(responseMap);
                    
                    log.info("成功验证CT平台令牌并设置用户上下文，用户ID: {}, 用户名: {}, 登录名: {}, 用户角色: {}",
                            UserContext.getUserId(), UserContext.getUserName(), UserContext.getLoginName(), UserContext.getUserRoles());
                    
                    return true;
                } else {
                    log.warn("CT平台令牌无效或已过期");
                    return false;
                }
            }

            log.warn("验证CT平台令牌失败，响应状态码: {}, 响应体：{}", response.getStatusCode(), response.getBody());
            return false;
        } catch (Exception e) {
            log.error("验证CT平台令牌异常", e);
            return false;
        }
    }

    /**
     * 设置用户上下文信息
     *
     * @param responseMap 验证响应数据
     */
    private void setUserContext(Map<String, Object> responseMap) {
        // 设置用户名
        if (responseMap.containsKey("user_name")) {
            String userName = responseMap.get("user_name").toString();
            UserContext.setUserId(userName);
            UserContext.setUserName(userName);
            UserContext.setLoginName(userName);
        }

        // 设置用户角色
        if (responseMap.containsKey("authorities")) {
            List<String> authorities = (List<String>) responseMap.get("authorities");
            String roles = authorities != null && !authorities.isEmpty() ? 
                    String.join(",", authorities) : "普通用户";
            UserContext.setUserRoles(roles);
        } else {
            UserContext.setUserRoles("普通用户");
        }
    }

    /**
     * 检查token是否即将过期
     *
     * @param token CT平台的令牌
     * @param thresholdSeconds 过期阈值（秒），如果token在这个时间内过期则返回true
     * @return 如果token即将过期返回true，否则返回false
     */
    public boolean isTokenExpiringSoon(String token, long thresholdSeconds) {
        try {
            log.debug("检查CT平台令牌过期时间: {}", token);

            String checkTokenUrl = validationUrl + "?token=" + token;
            HttpHeaders headers = new HttpHeaders();
            headers.set("Content-Type", "application/json");
            HttpEntity<String> entity = new HttpEntity<>(headers);

            ResponseEntity<String> response = restTemplate.exchange(
                    checkTokenUrl,
                    HttpMethod.GET,
                    entity,
                    String.class
            );

            if (response.getStatusCode().is2xxSuccessful()) {
                Map<String, Object> responseMap = objectMapper.readValue(response.getBody(), Map.class);
                
                if (responseMap.containsKey("active") && Boolean.TRUE.equals(responseMap.get("active"))
                        && responseMap.containsKey("exp")) {
                    long expiration = ((Number) responseMap.get("exp")).longValue();
                    long currentTime = System.currentTimeMillis() / 1000; // 转换为秒
                    long timeToExpire = expiration - currentTime;
                    
                    log.debug("Token过期时间: {}, 当前时间: {}, 剩余时间: {}秒", expiration, currentTime, timeToExpire);
                    
                    return timeToExpire <= thresholdSeconds;
                }
            }
            
            return true; // 如果无法获取过期时间，认为即将过期
        } catch (Exception e) {
            log.error("检查CT平台令牌过期时间异常", e);
            return true; // 异常情况下认为即将过期
        }
    }
}
