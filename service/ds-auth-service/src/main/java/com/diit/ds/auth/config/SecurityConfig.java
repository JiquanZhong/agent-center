package com.diit.ds.auth.config;

import com.diit.ds.auth.filter.CTSSOAuthenticationFilter;
import com.diit.ds.auth.filter.JwtAuthenticationFilter;
import com.diit.ds.auth.filter.SSOAuthenticationFilter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.beans.factory.ObjectProvider;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Configuration
@EnableWebSecurity
public class SecurityConfig {

    private final ObjectProvider<JwtAuthenticationFilter> jwtAuthenticationFilterProvider;
    private final ObjectProvider<SSOAuthenticationFilter> ssoAuthenticationFilterProvider;
    private final ObjectProvider<CTSSOAuthenticationFilter> ctSsoAuthenticationFilterProvider;

    @Value("${diit.security.auth-type:JWT}")
    private String authType;

    public SecurityConfig(
            ObjectProvider<JwtAuthenticationFilter> jwtAuthenticationFilterProvider,
            ObjectProvider<SSOAuthenticationFilter> ssoAuthenticationFilterProvider,
            ObjectProvider<CTSSOAuthenticationFilter> ctSsoAuthenticationFilterProvider) {
        this.jwtAuthenticationFilterProvider = jwtAuthenticationFilterProvider;
        this.ssoAuthenticationFilterProvider = ssoAuthenticationFilterProvider;
        this.ctSsoAuthenticationFilterProvider = ctSsoAuthenticationFilterProvider;
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .csrf(csrf -> csrf.disable())  // 禁用CSRF
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))  // 使用无状态会话
                .authorizeHttpRequests(auth -> auth
                        .anyRequest().permitAll()  // 允许所有请求通过Spring Security，实际验证由我们的过滤器负责
                );

        // 根据配置选择认证过滤器
        if ("SSO".equalsIgnoreCase(authType)) {
            SSOAuthenticationFilter ssoFilter = ssoAuthenticationFilterProvider.getIfAvailable();
            if (ssoFilter != null) {
                http.addFilterBefore(ssoFilter, UsernamePasswordAuthenticationFilter.class);
                log.info("使用SSO认证过滤器");
            } else {
                log.error("SSO认证过滤器未找到，请检查配置");
            }
        } else if ("CTSSO".equalsIgnoreCase(authType)) {
            CTSSOAuthenticationFilter ctSsoFilter = ctSsoAuthenticationFilterProvider.getIfAvailable();
            if(ctSsoFilter != null) {
                http.addFilterBefore(ctSsoFilter, UsernamePasswordAuthenticationFilter.class);
                log.info("使用CTSSO认证过滤器");
            }
        } else {
            // 默认使用JWT认证
            JwtAuthenticationFilter jwtFilter = jwtAuthenticationFilterProvider.getIfAvailable();
            if (jwtFilter != null) {
                http.addFilterBefore(jwtFilter, UsernamePasswordAuthenticationFilter.class);
                log.info("使用JWT认证过滤器");
            } else {
                log.error("JWT认证过滤器未找到，请检查配置");
            }
        }

        return http.build();
    }
} 