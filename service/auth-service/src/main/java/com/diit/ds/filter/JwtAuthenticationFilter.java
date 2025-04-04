package com.diit.ds.filter;

import com.diit.ds.annotation.NotNeedAuth;
import com.diit.ds.context.UserContext;
import com.diit.ds.util.JwtUtil;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerExecutionChain;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping;

import java.io.IOException;
import java.lang.reflect.Method;
import java.util.Map;

/**
 * JWT认证过滤器
 * 用于拦截请求并验证JWT令牌
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtUtil jwtUtil;
    private final RequestMappingHandlerMapping handlerMapping;
    @Value("${diit.security.enabled}")
    private Boolean enabled;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
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

            // 验证JWT令牌
            if (!jwtUtil.validateToken(token)) {
                log.warn("无效的JWT令牌: {}", request.getRequestURI());
                response.setStatus(HttpStatus.UNAUTHORIZED.value());
                response.getWriter().write("无效的JWT令牌");
                return;
            }

            // 解析JWT中的用户信息并存储到上下文中
            Map<String, String> userInfo = jwtUtil.parseUserInfo(token);
            if (userInfo != null) {
                // 设置用户上下文
                if (userInfo.containsKey("userName")) {
                    UserContext.setUserName(userInfo.get("userName"));
                }
                if (userInfo.containsKey("userId")) {
                    UserContext.setUserId(userInfo.get("userId"));
                }
                if (userInfo.containsKey("loginName")) {
                    UserContext.setLoginName(userInfo.get("loginName"));
                }

                log.debug("已设置用户上下文: {}", UserContext.getAttributes());
            } else {
                log.warn("无法解析JWT中的用户信息: {}", token);
            }

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
} 