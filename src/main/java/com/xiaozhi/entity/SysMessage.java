package com.xiaozhi.entity;

/**
 * 聊天记录表
 * 
 * @author Joey
 * 
 */
public class SysMessage extends SysDevice {
    /**
     * 消息类型 - 普通消息
     */
    public static final String MESSAGE_TYPE_NORMAL = "NORMAL";
    /**
     * 消息类型 - 函数调用消息
     */
    public static final String MESSAGE_TYPE_FUNCTION_CALL = "FUNCTION_CALL";
    /**
     * 消息类型 - MCP消息
     */
    public static final String MESSAGE_TYPE_MCP = "MCP";

    private Integer messageId;

    private String deviceId;

    private String sessionId;

    /**
     * 消息发送方：user-用户，ai-人工智能
     */
    private String sender;

    /**
     * 消息内容
     */
    private String message;

    /**
     * 语音文件路径
     */
    private String audioPath;

    /**
     * 语音状态
     * 
     */
    private String state;

    /**
     * 消息类型: NORMAL-普通消息，FUNCTION_CALL-函数调用消息，MCP-MCP调用消息
     *
     */
    private String messageType = "NORMAL";

    public String getDeviceId() {
        return deviceId;
    }

    public SysMessage setDeviceId(String deviceId) {
        this.deviceId = deviceId;
        return this;
    }

    public Integer getMessageId() {
        return this.messageId;
    }

    public SysMessage setMessageId(Integer messageId) {
        this.messageId = messageId;
        return this;
    }

    public String getSender() {
        return this.sender;
    }

    public SysMessage setSender(String sender) {
        this.sender = sender;
        return this;
    }

    public String getMessage() {
        return this.message;
    }

    public SysMessage setMessage(String message) {
        this.message = message;
        return this;
    }

    public String getAudioPath() {
        return audioPath;
    }

    public SysMessage setAudioPath(String audioPath) {
        this.audioPath = audioPath;
        return this;
    }

    public String getState() {
        return state;
    }

    public SysMessage setState(String state) {
        this.state = state;
        return this;
    }

    public String getMessageType() {
        return messageType;
    }

    public void setMessageType(String messageType) {
        this.messageType = messageType;
    }

    @Override
    public String toString() {
        return "SysMessage [deviceId=" + deviceId + ", sessionId=" + sessionId + ", messageId=" + messageId
                + ", sender=" + sender + ", message="
                + message + ", audioPath=" + audioPath + ", state=" + state + ", messageType=" + messageType + "]";
    }

}