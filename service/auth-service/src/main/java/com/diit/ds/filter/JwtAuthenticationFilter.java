package com.diit.ds.filter;

import com.diit.ds.annotation.*;
import com.diit.ds.annotation.JwtAuthTypeCondition;
import com.diit.ds.context.UserContext;
import com.diit.ds.util.JwtUtil;
import com.github.benmanes.caffeine.cache.Cache;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
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
import java.util.Arrays;
import java.util.Map;

/**
 * JWT认证过滤器
 * 用于拦截请求并验证JWT令牌
 */
@Slf4j
@Component
@JwtAuthTypeCondition
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtUtil jwtUtil;

    private final Cache<String, Map<String, String>> userInfoCache;
    private final RequestMappingHandlerMapping handlerMapping;

    /**
     * 是否启用安全认证
     */
    public JwtAuthenticationFilter(final JwtUtil jwtUtil,
                                   @Qualifier("jwtUserInfoCache") Cache<String, Map<String, String>> userInfoCache,
                                   @Qualifier("requestMappingHandlerMapping") RequestMappingHandlerMapping handlerMapping) {
        this.jwtUtil = jwtUtil;
        this.userInfoCache = userInfoCache;
        this.handlerMapping = handlerMapping;
    }

    @Value("${diit.security.enabled}")
    private Boolean enabled;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        try {
            // 清除上一个请求的用户上下文
            UserContext.clear();

            if (!enabled) {
                setDefaultUserContext();
                filterChain.doFilter(request, response);
                return;
            }

            // 检查是否是Swagger或Knife4j相关路径
            if (isSwaggerOrKnife4jPath(request)) {
                log.debug("Swagger或Knife4j相关路径放行: {} {}", request.getMethod(), request.getRequestURI());
                filterChain.doFilter(request, response);
                return;
            }

            // 检查是否是不需要认证的接口
            if (isNotNeedAuthEndpoint(request)) {
                log.debug("跳过认证: {} {}", request.getMethod(), request.getRequestURI());
                filterChain.doFilter(request, response);
                return;
            }

            // 获取Authorization请求头
            String authHeader = request.getHeader("Authorization");

            // 检查Authorization请求头是否存在且格式正确
            if (authHeader == null || !authHeader.startsWith("Bearer ")) {
                log.warn("未授权: 缺少有效的Authorization请求头: {}", request.getRequestURI());
                response.setStatus(HttpStatus.UNAUTHORIZED.value());
                response.getWriter().write("未授权: 缺少有效的Authorization请求头");
                return;
            }

            // 提取JWT令牌
            String token = authHeader.substring(7);

            // 首先从缓存中检查用户信息
            Map<String, String> userInfo = userInfoCache.getIfPresent(token);
            if (userInfo != null) {
                // 如果缓存中存在，直接设置用户上下文
                setUserContext(userInfo);
                filterChain.doFilter(request, response);
                return;
            }

            // 验证JWT令牌
            if (!jwtUtil.validateToken(token)) {
                log.warn("无效的JWT令牌: {}", request.getRequestURI());
                response.setStatus(HttpStatus.UNAUTHORIZED.value());
                response.getWriter().write("无效的JWT令牌");
                return;
            }

            // 解析JWT获取用户信息
            userInfo = jwtUtil.parseUserInfo(token);
            if (userInfo == null) {
                log.warn("无法解析JWT中的用户信息: {}", token);
                response.setStatus(HttpStatus.UNAUTHORIZED.value());
                response.getWriter().write("无效的JWT令牌：无法解析用户信息");
                return;
            }

            // 将用户信息存入缓存
            userInfoCache.put(token, userInfo);

            // 设置用户上下文
            setUserContext(userInfo);

            // 检查是否需要管理员权限
            if (isNeedAdminRoleEndpoint(request)) {
                String userRoles = UserContext.getUserRoles();
                if (userRoles == null) {
                    log.warn("需要管理员权限: {}", request.getRequestURI());
                    response.setStatus(HttpStatus.FORBIDDEN.value());
                    response.getWriter().write("需要管理员权限");
                    return;
                }
                String[] roles = userRoles.split(",");
                if (!Arrays.asList(roles).contains("管理员")) {
                    log.warn("需要管理员权限: {}", request.getRequestURI());
                    response.setStatus(HttpStatus.FORBIDDEN.value());
                    response.getWriter().write("需要管理员权限");
                    return;
                }
            }

            // 继续请求处理
            filterChain.doFilter(request, response);
        } catch (Exception e) {
            log.info("服务器内部错误: " + e.getMessage());
            response.setStatus(HttpStatus.INTERNAL_SERVER_ERROR.value());
            response.getWriter().write("服务器内部错误: " + e.getMessage());
        } finally {
            // 清除用户上下文，防止内存泄漏
            UserContext.clear();
        }
    }

    /**
     * 设置用户上下文信息
     *
     * @param userInfo 用户信息
     */
    private void setUserContext(Map<String, String> userInfo) {
        if (userInfo.containsKey("userName")) {
            UserContext.setUserName(userInfo.get("userName"));
        }
        if (userInfo.containsKey("userId")) {
            UserContext.setUserId(userInfo.get("userId"));
        }
        if (userInfo.containsKey("loginName")) {
            UserContext.setLoginName(userInfo.get("loginName"));
        }
        if (userInfo.containsKey("userRoles")) {
            UserContext.setUserRoles(userInfo.get("userRoles"));
        }
        log.debug("已设置用户上下文: {}", UserContext.getAttributes());
    }

    /**
     * 设置用户上下文信息
     *
     */
    private void setDefaultUserContext() {
        UserContext.setUserName("普通用户");
        UserContext.setUserId("1");
        UserContext.setLoginName("user");
        UserContext.setUserRoles("普通用户");
        log.debug("已设置用户上下文: {}", UserContext.getAttributes());
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
     * 判断是否需要管理员权限的接口
     *
     * @param request 请求
     * @return 是否需要管理员权限的接口
     */
    private boolean isNeedAdminRoleEndpoint(HttpServletRequest request) {
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

            // 检查方法上是否有NeedAdminRole注解
            if (method.isAnnotationPresent(NeedAdminRole.class)) {
                return true;
            }

            // 检查类上是否有NeedAdminRole注解
            return handlerMethod.getBeanType().isAnnotationPresent(NeedAdminRole.class);
        } catch (Exception e) {
            log.error("检查管理员权限要求时发生异常: {}", e.getMessage(), e);
            return false;
        }
    }
} 