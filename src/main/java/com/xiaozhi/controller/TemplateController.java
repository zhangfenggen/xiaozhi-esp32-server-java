package com.xiaozhi.controller;

import com.github.pagehelper.PageInfo;
import com.xiaozhi.common.web.AjaxResult;
import com.xiaozhi.entity.SysTemplate;
import com.xiaozhi.entity.SysUser;
import com.xiaozhi.service.SysTemplateService;
import com.xiaozhi.utils.CmsUtils;

import reactor.core.publisher.Mono;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ServerWebExchange;

import java.util.List;

/**
 * 提示词模板控制器
 */
@RestController
@RequestMapping("/api/template")
public class TemplateController {

    @Autowired
    private SysTemplateService templateService;

    /**
     * 查询模板列表
     */
    @GetMapping("/query")
    public Mono<AjaxResult> query(SysTemplate template, ServerWebExchange exchange) {
        return Mono.fromCallable(() -> {
            try {
                // 从请求属性中获取用户信息
                SysUser user = exchange.getAttribute(CmsUtils.USER_ATTRIBUTE_KEY);
                if (user != null) {
                    template.setUserId(user.getUserId());
                }
                List<SysTemplate> templateList = templateService.query(template);
                AjaxResult result = AjaxResult.success();
                result.put("data", new PageInfo<>(templateList));
                return result;
            } catch (Exception e) {
                return AjaxResult.error(e.getMessage());
            }
        });

    }

    /**
     * 添加模板
     */
    @PostMapping("/add")
    public Mono<AjaxResult> add(SysTemplate template, ServerWebExchange exchange) {
        return Mono.fromCallable(() -> {
            try {
            // 从请求属性中获取用户信息
            SysUser user = exchange.getAttribute(CmsUtils.USER_ATTRIBUTE_KEY);
            if (user != null) {
                template.setUserId(user.getUserId());
            }
            int rows = templateService.add(template);
            return rows > 0 ? AjaxResult.success() : AjaxResult.error("添加模板失败");
            } catch (Exception e) {
                return AjaxResult.error(e.getMessage());
            }
        });
    }

    /**
     * 修改模板
     */
    @PostMapping("/update")
    public Mono<AjaxResult> update(SysTemplate template, ServerWebExchange exchange) {
        return Mono.fromCallable(() -> {
            try {
                // 从请求属性中获取用户信息
                SysUser user = exchange.getAttribute(CmsUtils.USER_ATTRIBUTE_KEY);
                if (user != null) {
                    template.setUserId(user.getUserId());
                }
                int rows = templateService.update(template);
                return rows > 0 ? AjaxResult.success() : AjaxResult.error("修改模板失败");
            } catch (Exception e) {
                return AjaxResult.error(e.getMessage());
            }
        });
    }

}