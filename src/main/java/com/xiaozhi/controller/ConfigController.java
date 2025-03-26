package com.xiaozhi.controller;

import java.util.List;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;

import com.github.pagehelper.PageInfo;
import com.xiaozhi.common.web.AjaxResult;
import com.xiaozhi.entity.SysConfig;
import com.xiaozhi.service.SysConfigService;
import com.xiaozhi.utils.CmsUtils;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 设备管理
 * 
 * @author Joey
 * 
 */

@RestController
@RequestMapping("/api/config")
public class ConfigController {

    private static final Logger log = LoggerFactory.getLogger(ConfigController.class);

    @Resource
    private SysConfigService configService;

    /**
     * 配置查询
     * 
     * @param config
     * @return configList
     */
    @GetMapping("/query")
    public AjaxResult query(SysConfig config, HttpServletRequest request) {
        try {
            config.setUserId(CmsUtils.getUserId(request));
            List<SysConfig> deviceList = configService.query(config);
            AjaxResult result = AjaxResult.success();
            result.put("data", new PageInfo<>(deviceList));
            return result;
        } catch (Exception e) {
            log.error(e.getMessage(), e);
            return AjaxResult.error();
        }

    }

    /**
     * 配置信息更新
     * 
     * @param config
     * @return
     */
    @PostMapping("/update")
    public AjaxResult update(SysConfig config, HttpServletRequest request) {
        try {
            configService.update(config);
            return AjaxResult.success();
        } catch (Exception e) {
            log.error(e.getMessage(), e);
            return AjaxResult.error();
        }

    }

    /**
     * 添加配置
     * 
     * @param config
     */
    @PostMapping("/add")
    public AjaxResult add(SysConfig config, HttpServletRequest request) {
        try {
            config.setUserId(CmsUtils.getUserId(request));
            configService.add(config);
            return AjaxResult.success();
        } catch (Exception e) {
            log.error(e.getMessage(), e);
            return AjaxResult.error();
        }
    }

}