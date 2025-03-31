package com.diit.ds.util;

import com.diit.ds.domain.req.DifyAuthReq;
import com.diit.ds.domain.resp.DifyAuthResp;
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
public class DifyAuthUtil {

    private final RestTemplate restTemplate;

    @Value("${dify.signature}")
    private String signature;

    @Value("${dify.api.base-url}")
    private String baseUrl;

    private String accessToken = "";
    private Date expireTime = null;
    private String loginPath = "/console/api/login";

    /**
     * 获取有效的access token
     * 如果token不存在或已过期，则重新获取
     *
     * @return 有效的access token
     */
    public String getAccessToken() {
        log.debug("获取Dify访问令牌...");
        if (hasAccessToken() && !isExpired()) {
            log.debug("使用现有的有效访问令牌");
            return this.accessToken;
        }

        log.info("Dify访问令牌不存在或已过期，开始重新获取...");
        return refreshAccessToken();
    }

    /**
     * 刷新access token
     *
     * @return 新的access token
     */
    private String refreshAccessToken() {
        try {
            log.debug("开始调用Dify认证接口获取新的访问令牌");

            // 构建认证请求
            DifyAuthReq authReq = DifyAuthReq.builder()
                    .email("zhongjq@diit.cn")
                    .password("zxc15089860528")
                    .language("zh-Hans")
                    .rememberMe("true")
                    .build();

            // 设置请求头
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);

            // 构建请求体
            HttpEntity<DifyAuthReq> requestEntity = new HttpEntity<>(authReq, headers);

            // 拼接完整URL
            String url = baseUrl + loginPath;
            
            // 发送请求
            log.debug("认证请求URL: {}, 请求参数: {}", url, authReq);

            DifyAuthResp authResp = restTemplate.postForObject(url, requestEntity, DifyAuthResp.class);

            // 验证响应
            if (authResp == null) {
                log.error("Dify认证接口返回null");
                return "";
            }

            if (!authResp.getResult().equals("success")) {
                log.error("Dify认证接口请求失败，错误码: {}", authResp.getResult());
                return "";
            }

            if (authResp.getData() == null) {
                log.error("Dify认证接口返回的数据为null");
                return "";
            }

            // 更新token和过期时间
            this.accessToken = authResp.getData().getAccessToken();

            // 从JWT中提取过期时间
            try {
                // 假设JWT的payload部分包含exp字段，表示Unix时间戳格式的过期时间
                String[] jwtParts = this.accessToken.split("\\.");
                if (jwtParts.length == 3) {
                    // 解析JWT payload (第二部分)
                    String payload = new String(java.util.Base64.getUrlDecoder().decode(jwtParts[1]));

                    // 使用简单的字符串处理提取exp
                    if (payload.contains("\"exp\":")) {
                        int expStart = payload.indexOf("\"exp\":") + 6;
                        int expEnd = payload.indexOf(",", expStart);
                        if (expEnd == -1) {
                            expEnd = payload.indexOf("}", expStart);
                        }
                        String expValue = payload.substring(expStart, expEnd).trim();

                        // 将exp时间戳转换为Date对象
                        long expTimestamp = Long.parseLong(expValue) * 1000; // 转换为毫秒
                        this.expireTime = new Date(expTimestamp);
                        log.info("成功解析JWT过期时间，将在 {} 过期", this.expireTime);
                    } else {
                        throw new Exception("JWT中未找到exp字段");
                    }
                } else {
                    throw new Exception("JWT格式不正确");
                }
            } catch (Exception e) {
                log.error("解析JWT过期时间异常: {}", e.getMessage(), e);
                // 设置默认过期时间为24小时后
                this.expireTime = new Date(System.currentTimeMillis() + 24 * 3600 * 1000);
                log.info("设置默认过期时间为24小时后: {}", this.expireTime);
            }

            return this.accessToken;
        } catch (Exception e) {
            log.error("获取Dify访问令牌异常: {}", e.getMessage(), e);
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
            log.debug("Dify访问令牌已过期或即将过期");
        }

        return expired;
    }
}
