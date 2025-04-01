package com.xiaozhi.controller;

import java.util.List;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;

import com.github.pagehelper.PageInfo;
import com.xiaozhi.common.web.AjaxResult;
import com.xiaozhi.entity.SysConfig;
import com.xiaozhi.entity.SysRole;
import com.xiaozhi.service.SysConfigService;
import com.xiaozhi.service.SysRoleService;
import com.xiaozhi.utils.CmsUtils;
import com.xiaozhi.websocket.service.TextToSpeechService;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 角色管理
 * 
 * @author Joey
 * 
 */

@RestController
@RequestMapping("/api/role")
public class RoleController {

    private static final Logger log = LoggerFactory.getLogger(RoleController.class);

    @Resource
    private SysRoleService roleService;

    @Resource
    private TextToSpeechService textToSpeechService;

    @Resource
    private SysConfigService configService;

    /**
     * 角色查询
     * 
     * @param role
     * @return roleList
     */
    @GetMapping("/query")
    public AjaxResult query(SysRole role, HttpServletRequest request) {
        try {
            role.setUserId(CmsUtils.getUserId(request));
            List<SysRole> roleList = roleService.query(role);
            AjaxResult result = AjaxResult.success();
            result.put("data", new PageInfo<>(roleList));
            return result;
        } catch (Exception e) {
            log.error(e.getMessage(), e);
            return AjaxResult.error();
        }

    }

    /**
     * 角色信息更新
     * 
     * @param role
     * @return
     */
    @PostMapping("/update")
    public AjaxResult update(SysRole role, HttpServletRequest request) {
        try {
            roleService.update(role);
            return AjaxResult.success();
        } catch (Exception e) {
            log.error(e.getMessage(), e);
            return AjaxResult.error();
        }

    }

    /**
     * 添加角色
     * 
     * @param role
     */
    @PostMapping("/add")
    public AjaxResult add(SysRole role, HttpServletRequest request) {
        try {
            role.setUserId(CmsUtils.getUserId(request));
            roleService.add(role);
            return AjaxResult.success();
        } catch (Exception e) {
            log.error(e.getMessage(), e);
            return AjaxResult.error();
        }
    }

    @GetMapping("/testVoice")
    public AjaxResult testAudio(String message, String provider, String voiceName, HttpServletRequest request) {

        try {
            SysConfig config = configService.query(new SysConfig().setUserId(CmsUtils.getUserId(request)).setProvider(provider).setConfigType("tts")).get(0);

            String audioFilePath = textToSpeechService.textToSpeech(message, config, voiceName);
            AjaxResult result = AjaxResult.success();
            result.put("data", audioFilePath);
            return result;
        } catch (IndexOutOfBoundsException e) {
            return AjaxResult.error("请先到语音合成配置页面配置对应Key");
        } catch (Exception e) {
            log.error(e.getMessage(), e);
            return AjaxResult.error();
        }
    }

}