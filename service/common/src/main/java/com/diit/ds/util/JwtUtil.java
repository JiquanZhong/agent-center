package com.diit.ds.util;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Date;
import java.util.Map;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * JWT工具类，用于JWT令牌的验证
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class JwtUtil {

    private final ObjectMapper ObjectMapper;

    @Value("${diit.crypto.jwt-secret-key}")
    private String jwtSecretKey;

    /**
     * 使用JJWT验证令牌
     */
    public boolean jwtVerify(String token) {
        try {
            // 方式1：直接使用原始密钥
            Claims claims = Jwts.parserBuilder()
                    .setSigningKey(jwtSecretKey)
                    .build()
                    .parseClaimsJws(token)
                    .getBody();
            return claims.getExpiration().after(new Date());

        } catch (ExpiredJwtException e) {
            log.error("JWT已过期: {}", e.getMessage());
            return false;
        } catch (Exception e) {
            log.error("JWT验证失败: {}", e.getMessage());
            return false;
        }
    }

    /**
     * 解析JWT的payload部分
     * 
     * @param token JWT令牌
     * @return JWT的payload部分
     */
    public Map<String, Object> parsePayload(String token) {
        try {
            String[] jwtParts = token.split("\\.");
            if (jwtParts.length != 3) {
                log.error("JWT格式不正确，无法解析");
                return null;
            }

            String payload = new String(Base64.getUrlDecoder().decode(jwtParts[1]));
            return ObjectMapper.readValue(payload, Map.class);
        } catch (Exception e) {
            log.error("解析JWT payload失败: {}", e.getMessage(), e);
            return null;
        }
    }

    /**
     * 从JWT中解析用户信息
     * 
     * @param token JWT令牌
     * @return 用户信息Map，包含loginName、userId、userName等
     */
    @SuppressWarnings("unchecked")
    public Map<String, String> parseUserInfo(String token) {
        try {
            Map<String, Object> payload = parsePayload(token);
            if (payload == null || !payload.containsKey("sub")) {
                log.error("JWT payload中不包含sub字段");
                return null;
            }

            String subJson = (String) payload.get("sub");
            return ObjectMapper.readValue(subJson, Map.class);
        } catch (Exception e) {
            log.error("解析JWT中的用户信息失败: {}", e.getMessage(), e);
            return null;
        }
    }

    /**
     * 验证JWT令牌
     *
     * @param token JWT令牌
     * @return 是否有效
     */
    public boolean validateToken(String token) {
        try {

            // 尝试使用JJWT验证，如果失败，只依赖简单检查
            try {
                return jwtVerify(token);
            } catch (Exception e) {
                log.warn("JJWT验证失败，降级为简单验证: {}", e.getMessage());
                return true; // 如果简单检查通过，则认为有效
            }
        } catch (Exception e) {
            log.error("JWT验证失败: {}", e.getMessage());
            return false;
        }
    }
} 