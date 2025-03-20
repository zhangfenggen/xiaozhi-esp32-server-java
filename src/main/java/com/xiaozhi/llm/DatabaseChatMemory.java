package com.xiaozhi.llm;

import java.lang.reflect.Field;
import java.util.List;

import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import com.agentsflex.core.memory.ChatMemory;
import com.agentsflex.core.message.HumanMessage;
import com.agentsflex.core.message.Message;
import com.xiaozhi.entity.SysMessage;
import com.xiaozhi.service.SysMessageService;

@Component
@Scope("prototype")
public class DatabaseChatMemory implements ChatMemory {

    private SysMessageService messageService;

    private String deviceId;

    public DatabaseChatMemory setDeviceId(String deviceId) {
        this.deviceId = deviceId;
        return this;
    }

    public void setMessageService(SysMessageService messageService) {
        this.messageService = messageService;
    }

    @Override
    public List<Message> getMessages() {

        if (messageService == null) {
            throw new IllegalStateException("messageService is not initialized");
        }
        if (deviceId == null || deviceId.isEmpty()) {
            throw new IllegalArgumentException("deviceId is not set");
        }

        // 从数据库查询所有的历史消息
        List<SysMessage> messages = messageService.query(new SysMessage().setDeviceId(deviceId));
        if (messages != null) {
            for (SysMessage message : messages) {
                if (message.getSender().equals("user")) {
                    // 如果是用户消息，就创建一个HumanMessage对象
                    new HumanMessage(message.getMessage());
                    printFields(new HumanMessage(message.getMessage()));
                } else if (message.getSender().equals("assistant")) {
                    // 如果是助手消息，就创建一个AssistantMessage对象
                    // new AiMessage(message.getMessage());
                }
            }
        }

        return null;
    }

    @Override
    public void addMessage(Message message) {
        // 把消息添加到数据库
        System.out.println("addMessage: " + message.getMessageContent());
    }

    @Override
    public Object id() {
        return null;
    }

    public static void printFields(Object obj) {
        Class<?> clazz = obj.getClass();
        Field[] fields = clazz.getDeclaredFields();
        for (Field field : fields) {
            field.setAccessible(true); // 允许访问私有字段
            try {
                System.out.println("field======= " + field.getName() + " = " + field.get(obj));
            } catch (IllegalAccessException e) {
                e.printStackTrace();
            }
        }
    }
}
