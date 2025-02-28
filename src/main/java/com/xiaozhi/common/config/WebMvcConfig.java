package com.xiaozhi.common.config;

import com.xiaozhi.common.interceptor.AuthenticationInterceptor;
import com.xiaozhi.common.interceptor.LogInterceptor;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurationSupport;

import javax.annotation.Resource;
import java.util.*;

@Configuration
public class WebMvcConfig extends WebMvcConfigurationSupport {

    @Resource
    private LogInterceptor logInterceptor;

    @Resource
    private AuthenticationInterceptor authenticationInterceptor;

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        List<String> excludePathPatterns = new ArrayList<>();
        excludePathPatterns.add("/api/user/login");
        excludePathPatterns.add("/api/user/update");
        excludePathPatterns.add("/system/**");
        registry.addInterceptor(authenticationInterceptor).addPathPatterns("/api/**")
                .excludePathPatterns(excludePathPatterns).addPathPatterns("/system/uploads");
        registry.addInterceptor(logInterceptor).addPathPatterns("/**");
    }
}