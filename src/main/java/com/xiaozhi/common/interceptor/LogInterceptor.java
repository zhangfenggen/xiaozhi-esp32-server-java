package com.xiaozhi.common.interceptor;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

/**
 * 系统日志拦截器
 * 
 * @author xiaozhi
 * 
 */
@Component
public class LogInterceptor implements HandlerInterceptor {

    private static final Logger log = LoggerFactory.getLogger(LogInterceptor.class);

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object hanObject)
            throws Exception {
        return true;
    }

    /*
     * @Override public void postHandle(HttpServletRequest request,
     * HttpServletResponse response, Object hanObject, ModelAndView modelAndView)
     * throws Exception { }
     * 
     * @Override public void afterCompletion(HttpServletRequest request,
     * HttpServletResponse response, Object handler, Exception ex) throws Exception
     * { }
     */
}