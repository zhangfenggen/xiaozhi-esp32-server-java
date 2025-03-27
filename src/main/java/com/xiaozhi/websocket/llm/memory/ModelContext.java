package com.xiaozhi.websocket.llm.memory;

/**
 * 模型上下文
 * 包含模型配置和记忆管理
 */
public class ModelContext {
    private final String deviceId;
    private final String sessionId;
    private final Integer roleId;
    private final String systemMessage;
    private final ChatMemory chatMemory;
    
    /**
     * 构造函数
     * 
     * @param deviceId 设备ID
     * @param sessionId 会话ID
     * @param roleId 角色ID
     * @param chatMemory 聊天记忆
     */
    public ModelContext(String deviceId, String sessionId, Integer roleId, ChatMemory chatMemory) {
        this.deviceId = deviceId;
        this.sessionId = sessionId;
        this.roleId = roleId;
        this.chatMemory = chatMemory;
        this.systemMessage = chatMemory.getSystemMessage(deviceId, roleId);
    }
    
    /**
     * 获取设备ID
     * 
     * @return 设备ID
     */
    public String getDeviceId() {
        return deviceId;
    }
    
    /**
     * 获取会话ID
     * 
     * @return 会话ID
     */
    public String getSessionId() {
        return sessionId;
    }
    
    /**
     * 获取角色ID
     * 
     * @return 角色ID
     */
    public Integer getRoleId() {
        return roleId;
    }
    
    /**
     * 获取系统消息
     * 
     * @return 系统消息
     */
    public String getSystemMessage() {
        return systemMessage;
    }
    
    /**
     * 添加用户消息
     * 
     * @param message 用户消息
     */
    public void addUserMessage(String message) {
        chatMemory.addMessage(deviceId, sessionId, "user", message, roleId);
    }
    
    /**
     * 添加助手消息
     * 
     * @param message 助手消息
     */
    public void addAssistantMessage(String message) {
        chatMemory.addMessage(deviceId, sessionId, "assistant", message, roleId);
    }
}