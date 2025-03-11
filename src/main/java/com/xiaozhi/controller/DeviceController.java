package com.xiaozhi.controller;

import java.util.List;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;

import com.github.pagehelper.PageInfo;
import com.xiaozhi.common.web.AjaxResult;
import com.xiaozhi.entity.SysDevice;
import com.xiaozhi.service.SysDeviceService;
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
@RequestMapping("/api/device")
public class DeviceController {

    private static final Logger log = LoggerFactory.getLogger(DeviceController.class);

    @Resource
    private SysDeviceService deviceService;

    /**
     * 设备查询
     * 
     * @param device
     * @return deviceList
     */
    @GetMapping("/query")
    public AjaxResult query(SysDevice device, HttpServletRequest request) {
        try {
            device.setUserId(CmsUtils.getUserId(request));
            List<SysDevice> deviceList = deviceService.query(device);
            AjaxResult result = AjaxResult.success();
            result.put("data", new PageInfo<>(deviceList));
            return result;
        } catch (Exception e) {
            log.error(e.getMessage(), e);
            return AjaxResult.error();
        }

    }

    /**
     * 设备信息更新
     * 
     * @param device
     * @return
     */
    @PostMapping("/update")
    public AjaxResult update(SysDevice device, HttpServletRequest request) {
        try {
            deviceService.update(device);
            return AjaxResult.success();
        } catch (Exception e) {
            log.error(e.getMessage(), e);
            return AjaxResult.error();
        }

    }

    /**
     * 添加设备
     * 
     * @param code
     */
    @PostMapping("/add")
    public AjaxResult add(String code, HttpServletRequest request) {
        try {
            SysDevice device = new SysDevice();
            device.setCode(code);
            SysDevice query = deviceService.queryVerifyCode(device);
            if (query == null) {
                return AjaxResult.error("无效验证码");
            }
            device.setUserId(CmsUtils.getUserId(request));
            System.out.println(query.toString());
            device.setDeviceId(query.getDeviceId());
            device.setDeviceName("小智");
            deviceService.add(device);
            return AjaxResult.success();
        } catch (Exception e) {
            log.error(e.getMessage(), e);
            return AjaxResult.error();
        }
    }

}