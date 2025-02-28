package com.xiaozhi.controller;

import com.github.pagehelper.PageInfo;
import com.xiaozhi.common.web.AjaxResult;
import com.xiaozhi.entity.*;
import com.xiaozhi.service.*;
import com.xiaozhi.ultis.CmsUtils;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.*;

import java.util.List;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;

/**
 * @Author: Joey
 * @Date: 2025/2/28 下午2:46
 * @Description:
 */

@RestController
@RequestMapping("/api/message")
public class MessageController {
    private static final Logger log = LoggerFactory.getLogger(MessageController.class);

    @Resource
    private SysMessageService messageService;

    /**
     * 查询对话
     *
     * @param message
     * @return
     */
    @GetMapping("/query")
    public AjaxResult query(SysMessage message, HttpServletRequest request) {
        try {
            message.setUserId(CmsUtils.getUserId(request));
            List<SysMessage> messageList = messageService.query(message);
            AjaxResult result = AjaxResult.success();
            result.put("data", new PageInfo<>(messageList));
            return result;
        } catch (Exception e) {
            log.error(e.getMessage(), e);
            return AjaxResult.error();
        }

    }

}
