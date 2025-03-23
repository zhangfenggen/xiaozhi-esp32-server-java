package com.xiaozhi.websocket.llm;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import com.agentsflex.core.memory.ChatMemory;
import com.agentsflex.core.message.AiMessage;
import com.agentsflex.core.message.HumanMessage;
import com.agentsflex.core.message.Message;
import com.xiaozhi.entity.SysDevice;
import com.xiaozhi.entity.SysMessage;
import com.xiaozhi.service.SysMessageService;
import com.xiaozhi.websocket.service.TextToSpeechService;

@Component
@Scope("prototype")
public class DatabaseChatMemory implements ChatMemory {

    private static final Logger logger = LoggerFactory.getLogger(DatabaseChatMemory.class);

    private SysMessageService messageService;
    private TextToSpeechService textToSpeechService;
    private SysDevice device;

    // 构造函数注入
    @Autowired
    public void setMessageService(SysMessageService messageService) {
        this.messageService = messageService;
    }

    @Autowired
    public void setTextToSpeechService(TextToSpeechService textToSpeechService) {
        this.textToSpeechService = textToSpeechService;
    }

    // 设置设备ID
    public DatabaseChatMemory setDevice(SysDevice device) {
        this.device = device;
        return this;
    }

    public String getDeviceId() {
        return this.device.getDeviceId();
    }

    @Override
    public Object id() {
        return this.device.getDeviceId();
    }

    @Override
    public List<Message> getMessages() {
        if (device == null || messageService == null) {
            return new ArrayList<>();
        }

        try {
            // 查询数据库中与设备ID相关的消息
            List<SysMessage> dbMessages = messageService.query(new SysMessage().setDeviceId(id().toString()));
            List<Message> messages = new ArrayList<>();

            // 将数据库消息转换为AgentsFlex消息
            for (SysMessage dbMessage : dbMessages) {
                Message message = convertToMessage(dbMessage);
                if (message != null) {
                    messages.add(message);
                }
            }

            logger.info("Retrieved {} messages for device {}", messages.size(), id().toString());
            return messages;
        } catch (Exception e) {
            return new ArrayList<>();
        }
    }

    @Override
    public void addMessage(Message message) {
        if (device == null || messageService == null || textToSpeechService == null) {
            return;
        }

        try {
            // 创建数据库消息对象
            SysMessage dbMessage = new SysMessage();
            dbMessage.setDeviceId(id().toString());
            dbMessage.setSessionId(device.getSessionId());
            // 根据消息类型设置发送者和内容
            if (message instanceof HumanMessage) {
                dbMessage.setSender("user");
            } else {
                dbMessage.setSender("assistant");
            }
            String text = message.getMessageContent().toString();
            dbMessage.setMessage(text);
            String audioFilePath = textToSpeechService.textToSpeech(text);
            dbMessage.setAudioPath(audioFilePath);
            dbMessage.setRoleId(device.getRoleId());
            // 保存消息到数据库
            messageService.add(dbMessage);
        } catch (Exception e) {
            logger.error("Error adding message for device {}: {}", id().toString(), e.getMessage(), e);
        }
    }

    /**
     * 将数据库消息转换为AgentsFlex消息
     */
    private Message convertToMessage(SysMessage dbMessage) {
        try {
            String sender = dbMessage.getSender();
            String content = dbMessage.getMessage();

            if ("user".equals(sender)) {
                return new HumanMessage(content);
            } else {
                AiMessage aiMessage = new AiMessage();
                // 使用反射设置fullContent字段
                Field fullContentField = AiMessage.class.getDeclaredField("fullContent");
                fullContentField.setAccessible(true);
                fullContentField.set(aiMessage, content);

                // 设置content字段
                Field contentField = aiMessage.getClass().getSuperclass().getDeclaredField("content");
                contentField.setAccessible(true);
                contentField.set(aiMessage, content);

                return aiMessage;
            }
        } catch (Exception e) {
            logger.error("Error converting database message to AgentsFlex message: {}", e.getMessage(), e);
            return null;
        }
    }

    /**
     * 使用反射打印对象的所有字段和值
     */
    private void printFields(Object obj) {
        logger.debug("Fields of {}:", obj.getClass().getSimpleName());
        Field[] fields = obj.getClass().getDeclaredFields();
        for (Field field : fields) {
            field.setAccessible(true);
            try {
                logger.debug("  {} = {}", field.getName(), field.get(obj));
            } catch (Exception e) {
                logger.debug("  {} = [Error accessing field]", field.getName());
            }
        }

        // 检查父类的字段
        Class<?> superClass = obj.getClass().getSuperclass();
        if (superClass != null && !superClass.equals(Object.class)) {
            logger.debug("Fields from superclass {}:", superClass.getSimpleName());
            Field[] superFields = superClass.getDeclaredFields();
            for (Field field : superFields) {
                field.setAccessible(true);
                try {
                    logger.debug("  {} = {}", field.getName(), field.get(obj));
                } catch (Exception e) {
                    logger.debug("  {} = [Error accessing field]", field.getName());
                }
            }
        }
    }
}
