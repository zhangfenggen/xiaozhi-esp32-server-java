package com.xiaozhi.entity;

import java.util.Date;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/**
 * 设备表
 * 
 * @author Joey
 * 
 */
@JsonIgnoreProperties({ "startTime", "endTime", "start", "limit", "userId", "code" })
public class SysDevice extends SysRole {

    private String deviceId;

    private String sessionId;

    private Integer modelId;

    private String modelName;

    private String modelDesc;

    /**
     * 设备名称
     */
    private String deviceName;

    /**
     * 设备状态
     */
    private String state;

    /**
     * 设备对话次数
     */
    private Integer totalMessage;

    /**
     * 验证码
     */
    private String code;

    /**
     * 音频文件
     */
    private String audioPath;

    /**
     * 最后在线时间
     */
    @JsonFormat(timezone = "GMT+8", pattern = "yyyy-MM-dd HH:mm:ss")
    private Date lastLogin;

    public Integer getModelId() {
        return modelId;
    }

    public SysDevice setModelId(Integer modelId) {
        this.modelId = modelId;
        return this;
    }

    public String getModelName() {
        return modelName;
    }

    public SysDevice setModelName(String modelName) {
        this.modelName = modelName;
        return this;
    }

    public String getModelDesc() {
        return modelDesc;
    }

    public SysDevice setModelDesc(String modelDesc) {
        this.modelDesc = modelDesc;
        return this;
    }

    public String getDeviceId() {
        return deviceId;
    }

    public SysDevice setDeviceId(String deviceId) {
        this.deviceId = deviceId;
        return this;
    }

    public String getSessionId() {
        return sessionId;
    }

    public SysDevice setSessionId(String sessionId) {
        this.sessionId = sessionId;
        return this;
    }

    public String getDeviceName() {
        return deviceName;
    }

    public SysDevice setDeviceName(String deviceName) {
        this.deviceName = deviceName;
        return this;
    }

    public String getState() {
        return state;
    }

    public SysDevice setState(String state) {
        this.state = state;
        return this;
    }

    public Integer getTotalMessage() {
        return totalMessage;
    }

    public SysDevice setTotalMessage(Integer totalMessage) {
        this.totalMessage = totalMessage;
        return this;
    }

    public String getCode() {
        return code;
    }

    public SysDevice setCode(String code) {
        this.code = code;
        return this;
    }

    public String getAudioPath() {
        return audioPath;
    }

    public SysDevice setAudioPath(String audioPath) {
        this.audioPath = audioPath;
        return this;
    }

    public Date getLastLogin() {
        return lastLogin;
    }

    public SysDevice setLastLogin(Date lastLogin) {
        this.lastLogin = lastLogin;
        return this;
    }

    @Override
    public String toString() {
        return "SysDevice [deviceId=" + deviceId + ", sessionId=" + sessionId + ", modelId=" + modelId + ", modelName="
                + modelName + ", modelDesc=" + modelDesc + ", deviceName=" + deviceName + ", state=" + state
                + ", totalMessage=" + totalMessage + ", code=" + code + ", audioPath=" + audioPath + ", lastLogin="
                + lastLogin + "]";
    }
}