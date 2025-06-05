package com.diit.ds.auth.util;

import com.diit.ds.auth.config.H3CKnowledgeConfig;
import com.diit.ds.auth.domain.req.H3CAuthReq;
import com.diit.ds.auth.domain.resp.H3CAuthResp;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.util.Date;

@Component
@Data
@RequiredArgsConstructor
@Slf4j
public class H3CUtil {
    private String accessToken = "";
    private Date expireTime = null;

    private final RestTemplate restTemplate;
    private final H3CKnowledgeConfig h3cConfig;

    @Value("${h3c.auth.client-id}")
    private String clientId;

    @Value("${h3c.auth.client-secret}")
    private String clientSecret;

    /**
     * 获取有效的access token
     * 如果token不存在或已过期，则重新获取
     *
     * @return 有效的access token
     */
    public String getAccessToken() {
        log.debug("获取H3C访问令牌...");
        if (hasAccessToken() && !isExpired()) {
            log.debug("使用现有的有效访问令牌");
            return this.accessToken;
        }

        log.info("H3C访问令牌不存在或已过期，开始重新获取...");
        return refreshAccessToken();
    }

    /**
     * 刷新access token
     *
     * @return 新的access token
     */
    private String refreshAccessToken() {
        try {
            log.debug("开始调用H3C认证接口获取新的访问令牌");

            // 构建认证请求
            H3CAuthReq authReq = H3CAuthReq.builder()
                    .grantType("client_credentials")
                    .clientId(clientId)
                    .clientSecret(clientSecret)
                    .build();

            // 设置请求头
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);

            // 构建请求体
            HttpEntity<H3CAuthReq> requestEntity = new HttpEntity<>(authReq, headers);

            // 发送请求
            String url = h3cConfig.getBaseUrl() + "/api/hub/open/v1/auth/token";
            log.debug("认证请求URL: {}, 请求参数: {}", url, authReq);

            H3CAuthResp authResp = restTemplate.postForObject(url, requestEntity, H3CAuthResp.class);

            // 验证响应
            if (authResp == null) {
                log.error("H3C认证接口返回null");
                return "";
            }

            if (authResp.getCode() != 200) {
                log.error("H3C认证接口请求失败，错误码: {}, 错误信息: {}", authResp.getCode(), authResp.getMsg());
                return "";
            }

            if (authResp.getData() == null) {
                log.error("H3C认证接口返回的数据为null");
                return "";
            }

            // 更新token和过期时间
            this.accessToken = authResp.getData().getAccessToken();

            // 将时间戳转换为Date对象
            try {
                this.expireTime = authResp.getData().getExpireTime();
                log.info("成功获取新的H3C访问令牌，将在 {} 过期", this.expireTime);
            } catch (NumberFormatException e) {
                log.error("无法解析过期时间: {}", authResp.getData().getExpireTime(), e);
                // 设置默认过期时间为1小时后
                this.expireTime = new Date(System.currentTimeMillis() + 24 * 3600 * 1000);
                log.info("设置默认过期时间为24小时后: {}", this.expireTime);
            }

            return this.accessToken;
        } catch (Exception e) {
            log.error("获取H3C访问令牌异常: {}", e.getMessage(), e);
            return "";
        }
    }

    /**
     * 检查是否有access token
     */
    private boolean hasAccessToken() {
        return accessToken != null && !accessToken.isEmpty();
    }

    /**
     * 检查access token是否过期
     */
    private boolean isExpired() {
        if (expireTime == null) {
            return true;
        }

        // 提前5分钟刷新token
        Date now = new Date(System.currentTimeMillis() + 5 * 60 * 1000);
        boolean expired = now.after(expireTime);

        if (expired) {
            log.debug("H3C访问令牌已过期或即将过期");
        }

        return expired;
    }
}
