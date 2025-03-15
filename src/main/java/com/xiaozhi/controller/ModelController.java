package com.xiaozhi.controller;

import java.util.List;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;

import com.github.pagehelper.PageInfo;
import com.xiaozhi.common.web.AjaxResult;
import com.xiaozhi.entity.SysModel;
import com.xiaozhi.service.SysModelService;
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
@RequestMapping("/api/model")
public class ModelController {

    private static final Logger log = LoggerFactory.getLogger(ModelController.class);

    @Resource
    private SysModelService modelService;

    /**
     * 模型查询
     * 
     * @param model
     * @return modelList
     */
    @GetMapping("/query")
    public AjaxResult query(SysModel model, HttpServletRequest request) {
        try {
            model.setUserId(CmsUtils.getUserId(request));
            List<SysModel> deviceList = modelService.query(model);
            AjaxResult result = AjaxResult.success();
            result.put("data", new PageInfo<>(deviceList));
            return result;
        } catch (Exception e) {
            log.error(e.getMessage(), e);
            return AjaxResult.error();
        }

    }

    /**
     * 模型信息更新
     * 
     * @param model
     * @return
     */
    @PostMapping("/update")
    public AjaxResult update(SysModel model, HttpServletRequest request) {
        try {
            modelService.update(model);
            return AjaxResult.success();
        } catch (Exception e) {
            log.error(e.getMessage(), e);
            return AjaxResult.error();
        }

    }

    /**
     * 添加角色
     * 
     * @param model
     */
    @PostMapping("/add")
    public AjaxResult add(SysModel model, HttpServletRequest request) {
        try {
            model.setUserId(CmsUtils.getUserId(request));
            modelService.add(model);
            return AjaxResult.success();
        } catch (Exception e) {
            log.error(e.getMessage(), e);
            return AjaxResult.error();
        }
    }

}