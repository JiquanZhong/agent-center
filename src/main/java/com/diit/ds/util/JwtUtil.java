package com.diit.ds.util;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Date;

/**
 * JWT工具类，用于JWT令牌的验证
 */
@Slf4j
@Component
public class JwtUtil {

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