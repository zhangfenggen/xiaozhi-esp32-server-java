package com.xiaozhi.websocket.llm;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import com.agentsflex.core.memory.ChatMemory;
import com.agentsflex.core.message.AiMessage;
import com.agentsflex.core.message.HumanMessage;
import com.agentsflex.core.message.Message;
import com.agentsflex.core.message.SystemMessage;
import com.xiaozhi.entity.SysDevice;
import com.xiaozhi.entity.SysMessage;
import com.xiaozhi.service.SysMessageService;
import com.xiaozhi.websocket.service.TextToSpeechService;

@Component
@Scope("prototype")
public class DatabaseChatMemory implements ChatMemory {

    private static final Logger logger = LoggerFactory.getLogger(DatabaseChatMemory.class);

    private final Object id = null;
    private final List<Message> messages = new ArrayList<>();

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

        messages.add(new SystemMessage(device.getRoleDesc()));

        try {
            Integer limit = device.getLimit();
            SysMessage queryMessage = new SysMessage();
            queryMessage.setDeviceId(device.getDeviceId());
            queryMessage.setLimit(limit);
            // 查询数据库中与设备ID相关的消息
            List<SysMessage> dbMessages = messageService
                    .query(queryMessage);
            // 翻转消息列表
            dbMessages = new ArrayList<>(dbMessages);
            dbMessages.sort((m1, m2) -> m1.getCreateTime().compareTo(m2.getCreateTime()));

            // 将数据库消息转换为AgentsFlex消息
            for (SysMessage dbMessage : dbMessages) {
                Message message = convertToMessage(dbMessage);
                if (message != null) {
                    messages.add(message);
                }
            }
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
            String text = null;
            // 根据消息类型设置发送者和内容
            if (message instanceof HumanMessage) {
                dbMessage.setSender("user");
                text = message.getMessageContent().toString();
            } else {
                dbMessage.setSender("assistant");
                AiMessage aiMessage = (AiMessage) message;
                Field fullContentField = AiMessage.class.getDeclaredField("fullContent");
                fullContentField.setAccessible(true);
                text = (String) fullContentField.get(aiMessage);
                if (text.isEmpty()) {
                    return;
                }
            }

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
                aiMessage.setFullContent(content);

                return aiMessage;
            }
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * 递归打印对象字段
     * 
     * @param obj     要打印的对象
     * @param prefix  前缀（用于缩进）
     * @param visited 已访问对象集合
     * @param depth   递归深度，防止过深递归
     */
    public static void printFieldsRecursive(Object obj, String prefix, Set<Object> visited, int depth) {
        if (obj == null || depth > 5) { // 限制递归深度为5
            return;
        }

        // 如果对象已被访问过，避免循环引用
        if (visited.contains(obj)) {
            logger.debug("{}= [已访问过的对象: {}]", prefix, obj.getClass().getSimpleName());
            return;
        }

        // 添加到已访问集合
        visited.add(obj);

        // 处理集合类型
        if (obj instanceof Collection<?>) {
            Collection<?> collection = (Collection<?>) obj;
            logger.debug("{}= Collection with {} elements", prefix, collection.size());
            int index = 0;
            for (Object item : collection) {
                if (index < 10) { // 限制只打印前10个元素
                    if (item != null) {
                        if (isPrimitiveOrString(item.getClass())) {
                            logger.debug("{}  [{}] = {}", prefix, index, item);
                        } else {
                            logger.debug("{}  [{}] = {}", prefix, index, item.getClass().getSimpleName());
                            printFieldsRecursive(item, prefix + "    ", visited, depth + 1);
                        }
                    } else {
                        logger.debug("{}  [{}] = null", prefix, index);
                    }
                }
                index++;
            }
            if (collection.size() > 10) {
                logger.debug("{}  ... and {} more elements", prefix, collection.size() - 10);
            }
            return;
        }

        // 处理Map类型
        if (obj instanceof Map<?, ?>) {
            Map<?, ?> map = (Map<?, ?>) obj;
            logger.debug("{}= Map with {} entries", prefix, map.size());
            int count = 0;
            for (Map.Entry<?, ?> entry : map.entrySet()) {
                if (count < 10) { // 限制只打印前10个元素
                    Object key = entry.getKey();
                    Object value = entry.getValue();
                    logger.debug("{}  Key: {}", prefix, key);
                    if (value != null) {
                        if (isPrimitiveOrString(value.getClass())) {
                            logger.debug("{}  Value: {}", prefix, value);
                        } else {
                            logger.debug("{}  Value: {}", prefix, value.getClass().getSimpleName());
                            printFieldsRecursive(value, prefix + "    ", visited, depth + 1);
                        }
                    } else {
                        logger.debug("{}  Value: null", prefix, value);
                    }
                }
                count++;
            }
            if (map.size() > 10) {
                logger.debug("{}  ... and {} more entries", prefix, map.size() - 10);
            }
            return;
        }

        // 处理普通对象
        Class<?> clazz = obj.getClass();
        logger.debug("{}Fields of {} ({})", prefix, clazz.getSimpleName(), obj);

        // 处理特殊的Message类型
        if (obj instanceof Message) {
            Message message = (Message) obj;
            logger.debug("{}  MessageType: {}", prefix, message.getClass().getSimpleName());
            logger.debug("{}  Content: {}", prefix, message.getMessageContent());
            return;
        }

        // 处理普通字段
        while (clazz != null && clazz != Object.class) {
            Field[] fields = clazz.getDeclaredFields();
            for (Field field : fields) {
                // 跳过静态字段和合成字段
                if (Modifier.isStatic(field.getModifiers()) || field.isSynthetic()) {
                    continue;
                }

                field.setAccessible(true);
                try {
                    Object value = field.get(obj);
                    String fieldName = field.getName();

                    if (value == null) {
                        logger.debug("{}  {} = null", prefix, fieldName);
                    } else if (isPrimitiveOrString(value.getClass())) {
                        logger.debug("{}  {} = {}", prefix, fieldName, value);
                    } else {
                        logger.debug("{}  {} = {} ({})", prefix, fieldName,
                                value.getClass().getSimpleName(), value);
                        printFieldsRecursive(value, prefix + "    ", visited, depth + 1);
                    }
                } catch (Exception e) {
                    logger.debug("{}  {} = [Error accessing field: {}]", prefix, field.getName(), e.getMessage());
                }
            }
            clazz = clazz.getSuperclass();
        }
    }

    /**
     * 判断一个类是否为基本类型或String
     */
    private static boolean isPrimitiveOrString(Class<?> clazz) {
        return clazz.isPrimitive() || clazz == String.class || clazz == Boolean.class ||
                clazz == Integer.class || clazz == Long.class || clazz == Float.class ||
                clazz == Double.class || clazz == Byte.class || clazz == Short.class ||
                clazz == Character.class || clazz.isEnum();
    }

}
