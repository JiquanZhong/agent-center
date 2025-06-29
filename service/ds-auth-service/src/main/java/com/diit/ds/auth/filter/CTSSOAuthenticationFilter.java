package com.diit.ds.auth.filter;

import com.diit.ds.auth.annotation.CTSSOAuthTypeCondition;
import com.diit.ds.auth.annotation.NotNeedAuth;
import com.diit.ds.auth.util.CTSSOUtil;
import com.diit.ds.common.context.UserContext;
import com.github.benmanes.caffeine.cache.Cache;
import jakarta.servlet.FilterChain;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerExecutionChain;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping;

import java.io.IOException;
import java.lang.reflect.Method;

/**
 * SSO认证过滤器
 * 用于拦截请求并验证TOKEN令牌
 */
@Slf4j
@Component
@CTSSOAuthTypeCondition
public class CTSSOAuthenticationFilter extends OncePerRequestFilter {

    private final CTSSOUtil ctssoUtil;
    private final Cache<String, Boolean> tokenCache;
    private final RequestMappingHandlerMapping handlerMapping;


    public CTSSOAuthenticationFilter(CTSSOUtil ctssoUtil,
                                     @Qualifier("ssoTokenCache") Cache<String, Boolean> tokenCache,
                                     @Qualifier("requestMappingHandlerMapping") RequestMappingHandlerMapping handlerMapping) {
        this.ctssoUtil = ctssoUtil;
        this.tokenCache = tokenCache;
        this.handlerMapping = handlerMapping;
    }

    @Value("${diit.security.enabled}")
    private Boolean enabled;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain) throws IOException {
        try {
            // 清除上一个请求的用户上下文
            UserContext.clear();

            if (!enabled) {
                filterChain.doFilter(request, response);
                return;
            }

            // 检查是否是Swagger或Knife4j相关路径
            if (isSwaggerOrKnife4jPath(request)) {
                log.debug("Swagger或Knife4j相关路径放行: {} {}", request.getMethod(), request.getRequestURI());
                filterChain.doFilter(request, response);
                return;
            }

            // 获取Authorization请求头
            String authHeader = request.getHeader("Authorization");
            if (authHeader == null || !authHeader.startsWith("Bearer ")) {
                response.setStatus(HttpStatus.UNAUTHORIZED.value());
                response.getWriter().write("未授权: 缺少或无效的令牌");
                return;
            }

            // 提取token令牌
            String token = authHeader.substring(7);

            // 首先从缓存中检查token
            Boolean isValid = tokenCache.getIfPresent(token);
            if (isValid != null && isValid) {
                // 如果缓存中存在且有效，直接放行
                filterChain.doFilter(request, response);
                return;
            }

            // 缓存中不存在或已失效，验证token令牌
            if (!ctssoUtil.validateExternalToken(token)) {
                log.warn("无效的令牌: {}", token);
                response.setStatus(HttpStatus.UNAUTHORIZED.value());
                response.getWriter().write("无效的令牌");
                return;
            }

            // 验证成功，将token存入缓存
            tokenCache.put(token, true);

            // 继续请求处理
            filterChain.doFilter(request, response);
        } catch (Exception e) {
            response.setStatus(HttpStatus.INTERNAL_SERVER_ERROR.value());
            response.getWriter().write("服务器内部错误: " + e.getMessage());
        } finally {
            // 清除用户上下文，防止内存泄漏
            UserContext.clear();
        }
    }

    /**
     * 判断请求是否为Swagger或Knife4j相关路径
     *
     * @param request HTTP请求
     * @return 是否为Swagger或Knife4j相关路径
     */
    private boolean isSwaggerOrKnife4jPath(HttpServletRequest request) {
        String path = request.getRequestURI();

        // 放行Swagger和Knife4j相关路径
        if (path.contains("/swagger-ui") ||
                path.contains("/v3/api-docs") ||
                path.contains("/swagger-resources") ||
                path.contains("/doc.html") ||
                path.contains("/webjars/") ||
                path.contains("/favicon.ico") ||
                path.contains("/actuator")) {
            return true;
        }

        return false;
    }

    /**
     * 判断是否是不需要认证的接口
     *
     * @param request 请求
     * @return 是否是不需要认证的接口
     */
    private boolean isNotNeedAuthEndpoint(HttpServletRequest request) {
        try {
            HandlerExecutionChain handlerExecutionChain = handlerMapping.getHandler(request);
            if (handlerExecutionChain == null) {
                return false;
            }

            Object handler = handlerExecutionChain.getHandler();
            if (!(handler instanceof HandlerMethod)) {
                return false;
            }

            HandlerMethod handlerMethod = (HandlerMethod) handler;
            Method method = handlerMethod.getMethod();

            // 检查方法上是否有NotNeedAuth注解
            if (method.isAnnotationPresent(NotNeedAuth.class)) {
                return true;
            }

            // 检查类上是否有NotNeedAuth注解
            return handlerMethod.getBeanType().isAnnotationPresent(NotNeedAuth.class);
        } catch (Exception e) {
            log.error("检查接口认证要求时发生异常: {}", e.getMessage(), e);
            return false;
        }
    }
} 