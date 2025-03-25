package com.xiaozhi.common.config;

import com.xiaozhi.common.interceptor.AuthenticationInterceptor;
import com.xiaozhi.common.interceptor.LogInterceptor;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurationSupport;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;

import javax.annotation.Resource;

import java.io.File;
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
        // Add exclusion for audio files
        excludePathPatterns.add("/audio/**");
        
        registry.addInterceptor(authenticationInterceptor)
                .addPathPatterns("/api/**")
                .excludePathPatterns(excludePathPatterns);
        
        registry.addInterceptor(logInterceptor).addPathPatterns("/**");
    }

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/**")
                .allowedOrigins("*")
                .allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS")
                .allowedHeaders("*");
    }

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        try {
            // 获取项目根目录的绝对路径
            String basePath = new File("").getAbsolutePath();
            
            // 音频文件存储在项目根目录下的audio文件夹中
            String audioPath = "file:" + basePath + File.separator + "audio" + File.separator;
            
            // 配置资源映射
            registry.addResourceHandler("/audio/**")
                    .addResourceLocations(audioPath);

            super.addResourceHandlers(registry);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}