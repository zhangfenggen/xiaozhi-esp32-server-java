package com.xiaozhi.common.interceptor;

import javax.annotation.Resource;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import com.xiaozhi.common.web.HttpStatus;
import com.xiaozhi.common.web.ResponseUtils;
import com.xiaozhi.entity.SysUser;
import com.xiaozhi.service.SysUserService;
import com.xiaozhi.utils.CmsUtils;

import net.sf.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.servlet.HandlerInterceptor;

@Component
public class AuthenticationInterceptor implements HandlerInterceptor {

    @Resource
    private SysUserService userService;

    private static final Logger log = LoggerFactory.getLogger(AuthenticationInterceptor.class);

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object hanObject) {
        HttpSession session = request.getSession();
        SysUser user = (SysUser) session.getAttribute(SysUserService.USER_SESSIONKEY);
        if (user == null) {
            Cookie[] cookie = request.getCookies();
            if (cookie != null && cookie.length > 0) {
                for (Cookie c : cookie) {
                    if (c.getName().equals("username")) {
                        user = userService.selectUserByUsername(c.getValue());
                        session.setAttribute(userService.USER_SESSIONKEY, user);
                        break;
                    }
                }
            }
        }
        CmsUtils.setUser(request, user);
        if (null == user) {
            String ajaxTag = request.getHeader("Request-By");// Ext
            String head = request.getHeader("X-Requested-With");// X-Requested-With
            if ((null != ajaxTag && ajaxTag.trim().equalsIgnoreCase("Ext"))
                    || (null != head && !(head.equalsIgnoreCase("XMLHttpRequest")))) {
                response.addHeader("_timeout", "true");
            } else {
                JSONObject obj = new JSONObject();
                obj.put("code", HttpStatus.FORBIDDEN);
                obj.put("message", "用户未登录.");
                ResponseUtils.renderJson(response, obj.toString());
            }
            log.error("用户未登录,或者session已经失效需重新登录");
            return false;
        }
        String msg = "";

        if (!StringUtils.isEmpty(msg)) {
            String ajaxTag = request.getHeader("Request-By");// Ext
            String head = request.getHeader("X-Requested-With");// X-Requested-With
            if ((ajaxTag != null && ajaxTag.trim().equalsIgnoreCase("Ext"))
                    || (head != null && !(head.equalsIgnoreCase("XMLHttpRequest")))) {
                response.addHeader("_timeout", "true");
            } else {
                JSONObject obj = new JSONObject();
                obj.put("code", HttpStatus.UNAUTHORIZED);
                obj.put("message", msg);
                ResponseUtils.renderJson(response, obj.toString());
            }
            return false;
        }
        return true;
    }

}