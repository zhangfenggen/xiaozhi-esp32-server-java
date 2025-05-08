package com.xiaozhi.websocket.llm.memory;

import com.xiaozhi.entity.Base;
import com.xiaozhi.entity.SysMessage;
import com.xiaozhi.entity.SysRole;
import com.xiaozhi.service.SysMessageService;
import com.xiaozhi.service.SysRoleService;
import com.xiaozhi.websocket.tts.factory.TtsServiceFactory;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 基于数据库的聊天记忆实现
 */
@Service
public class DatabaseChatMemory implements ChatMemory {
    private static final Logger logger = LoggerFactory.getLogger(DatabaseChatMemory.class);

    @Autowired
    private SysMessageService messageService;

    @Autowired
    private SysRoleService roleService;

    @Autowired
    private TtsServiceFactory ttsService;

    // 缓存系统消息，避免频繁查询数据库
    private Map<String, String> systemMessageCache = new ConcurrentHashMap<>();

    @Override
    public void addMessage(String deviceId, String sessionId, String sender, String content, Integer roleId, String messageType) {
        try {
            SysMessage message = new SysMessage();
            message.setDeviceId(deviceId);
            message.setSessionId(sessionId);
            message.setSender(sender);
            message.setMessage(content);
            message.setRoleId(roleId);
            message.setMessageType(messageType);
            if (sender == "assistant") {
                // 目前生成的语音保存采用默认的语音合成服务，后续可以考虑支持自定义语音合成服务
                // todo
                message.setAudioPath(ttsService.getDefaultTtsService().textToSpeech(content));
            }
            messageService.add(message);
        } catch (Exception e) {
            logger.error("保存消息时出错: {}", e.getMessage(), e);
        }
    }

    @Override
    public List<SysMessage> getMessages(String deviceId, String messageType, Integer limit) {
        try {
            SysMessage queryMessage = new SysMessage();
            queryMessage.setDeviceId(deviceId);
            queryMessage.setMessageType(messageType);
            queryMessage.setStart(1);
            queryMessage.setLimit(limit);

            List<SysMessage> messages = messageService.query(queryMessage);
            messages = new ArrayList<>(messages);
            messages.sort(Comparator.comparing(Base::getCreateTime));
            return messages;
            // return messages;
        } catch (Exception e) {
            logger.error("获取历史消息时出错: {}", e.getMessage(), e);
            return new ArrayList<>();
        }
    }

    @Override
    public void clearMessages(String deviceId) {
        try {
            // 清除设备的历史消息
            SysMessage deleteMessage = new SysMessage();
            deleteMessage.setDeviceId(deviceId);
            // messageService.update(deleteMessage);

            // 清除缓存
            systemMessageCache.keySet().removeIf(key -> key.startsWith(deviceId + ":"));
        } catch (Exception e) {
            logger.error("清除设备历史记录时出错: {}", e.getMessage(), e);
        }
    }

    @Override
    public String getSystemMessage(String deviceId, Integer roleId) {
        String cacheKey = deviceId + ":" + roleId;

        // 先从缓存获取
        if (systemMessageCache.containsKey(cacheKey)) {
            return systemMessageCache.get(cacheKey);
        }

        try {
            // 从数据库获取角色描述
            SysRole role = roleService.selectRoleById(roleId);
            if (role != null && role.getRoleDesc() != null) {
                String systemMessage = role.getRoleDesc();
                // 存入缓存
                systemMessageCache.put(cacheKey, systemMessage);
                return systemMessage;
            }
        } catch (Exception e) {
            logger.error("获取系统消息时出错: {}", e.getMessage(), e);
        }

        return "";
    }

    @Override
    public void setSystemMessage(String deviceId, Integer roleId, String systemMessage) {
        String cacheKey = deviceId + ":" + roleId;

        // 更新缓存
        systemMessageCache.put(cacheKey, systemMessage);

    }
}