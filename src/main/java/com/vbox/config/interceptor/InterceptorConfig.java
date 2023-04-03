package com.vbox.config.interceptor;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class InterceptorConfig implements WebMvcConfigurer {

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        AuthInterceptor authInterceptor = new AuthInterceptor();
        String[] path = {"/**"}; // 如果拦截全部可以设置为 /**
        String[] excludePath = {
                "/api/login",
                "/error",
                "/api/code/jx3/cap",
                "/api/code/test",
                "/api/test/test",
                "/api/test/callback",
                "/api/code/order/create",
                "/api/channel/order/**",
        }; // 不需要拦截的接口路径
        registry.addInterceptor(authInterceptor)
                .addPathPatterns(path)
                .excludePathPatterns(excludePath);
    }

}
