package com.xiaozhi.controller;

import java.io.BufferedReader;
import java.util.Date;
import java.util.List;
import java.util.Map;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;

import com.fasterxml.jackson.databind.ObjectMapper;
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
            device.setDeviceId(query.getDeviceId());
            device.setDeviceName("小智");
            deviceService.add(device);
            return AjaxResult.success();
        } catch (Exception e) {
            log.error(e.getMessage(), e);
            return AjaxResult.error();
        }
    }

    @PostMapping("/ota")
    public void ota(HttpServletRequest request) {
        try {

            SysDevice device = new SysDevice();

            // 获取device-id请求头
            String deviceId = request.getHeader("device-id");
            if (deviceId != null) {
                device.setDeviceId(deviceId);
            }

            // 读取请求体内容
            StringBuilder requestBody = new StringBuilder();
            BufferedReader reader = request.getReader();
            String line;
            while ((line = reader.readLine()) != null) {
                requestBody.append(line);
            }

            // 解析JSON请求体
            if (request.getContentType() != null && request.getContentType().contains("application/json")) {
                ObjectMapper mapper = new ObjectMapper();
                Map<String, Object> jsonData = mapper.readValue(requestBody.toString(), Map.class);

                // 提取chip_model_name
                if (jsonData.containsKey("chip_model_name")) {
                    device.setChipModelName((String) jsonData.get("chip_model_name"));
                }

                // 提取application.version
                if (jsonData.containsKey("application") && jsonData.get("application") instanceof Map) {
                    Map<String, Object> application = (Map<String, Object>) jsonData.get("application");
                    if (application.containsKey("version")) {
                        device.setVersion((String) application.get("version"));
                    }
                }

                // 提取board中的ssid和ip
                if (jsonData.containsKey("board") && jsonData.get("board") instanceof Map) {
                    Map<String, Object> board = (Map<String, Object>) jsonData.get("board");
                    if (board.containsKey("ssid")) {
                        device.setWifiName((String) board.get("ssid"));
                    }
                    if (board.containsKey("ip")) {
                        device.setIp((String) board.get("ip"));
                    }
                }

            }

            device.setState("1");
            device.setLastLogin(new Date().toString());
            deviceService.update(device);

        } catch (Exception e) {
            log.error(e.getMessage(), e);
        }
    }

}