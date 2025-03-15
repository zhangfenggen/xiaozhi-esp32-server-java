package com.xiaozhi.entity;

/**
 * 聊天记录表
 * 
 * @author Joey
 * 
 */
public class SysMessage extends SysDevice {
    private Integer messageId;

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

    public Integer getMessageId() {
        return this.messageId;
    }

    public void setMessageId(Integer messageId) {
        this.messageId = messageId;
    }

    public String getSender() {
        return this.sender;
    }

    public void setSender(String sender) {
        this.sender = sender;
    }

    public String getMessage() {
        return this.message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public String getAudioPath() {
        return audioPath;
    }

    public void setAudioPath(String audioPath) {
        this.audioPath = audioPath;
    }

    public String getState() {
        return state;
    }

    public void setState(String state) {
        this.state = state;
    }

}