package com.xiaozhi.entity;

/**
 * 聊天记录表
 * 
 * @author Joey
 * 
 */
public class SysMessage extends SysDevice {

    private Integer messageId;

    private String deviceId;

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

    @Override
    public String toString() {
        return "SysMessage [deviceId=" + deviceId + ", messageId=" + messageId + ", sender=" + sender + ", message="
                + message + ", audioPath=" + audioPath + ", state=" + state + "]";
    }

}