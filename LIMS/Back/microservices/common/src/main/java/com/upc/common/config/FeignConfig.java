package com.upc.common.config;

import com.upc.common.utils.UserContext;
import feign.RequestInterceptor;
import feign.RequestTemplate;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

/**
 * Feign配置类
 * 用于在服务间调用时传递用户上下文信息
 */
@Configuration
public class FeignConfig {

    @Bean
    public RequestInterceptor feignRequestInterceptor() {
        return new RequestInterceptor() {
            @Override
            public void apply(RequestTemplate template) {
                // 从当前请求上下文获取JWT Token
                ServletRequestAttributes attributes = 
                        (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
                if (attributes != null) {
                    HttpServletRequest request = attributes.getRequest();
                    String token = request.getHeader("Authorization");
                    if (token != null && !token.isEmpty()) {
                        template.header("Authorization", token);
                    }
                }

                // 传递用户上下文信息
                Integer userId = UserContext.getCurrentUserId();
                if (userId != null) {
                    template.header("X-User-Id", String.valueOf(userId));
                }

                String username = UserContext.getCurrentUserName();
                if (username != null) {
                    template.header("X-User-Name", username);
                }

                Integer role = UserContext.getCurrentUserRole();
                if (role != null) {
                    template.header("X-User-Role", String.valueOf(role));
                }
            }
        };
    }
}
