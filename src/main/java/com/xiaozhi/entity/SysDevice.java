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

    private Integer configId;

    private String configName;

    private String configDesc;

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
    private String lastLogin;

    /**
     * WiFi名称
     */
    private String wifiName;

    /**
     * IP
     */
    private String ip;

    /**
     * 芯片型号
     */
    private String chipModelName;

    /**
     * 固件版本
     */
    private String version;

    public Integer getConfigId() {
        return configId;
    }

    public SysDevice setConfigId(Integer configId) {
        this.configId = configId;
        return this;
    }

    public String getConfigName() {
        return configName;
    }

    public SysDevice setConfigName(String configName) {
        this.configName = configName;
        return this;
    }

    public String getConfigDesc() {
        return configDesc;
    }

    public SysDevice setConfigDesc(String configDesc) {
        this.configDesc = configDesc;
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

    public String getLastLogin() {
        return lastLogin;
    }

    public SysDevice setLastLogin(String lastLogin) {
        this.lastLogin = lastLogin;
        return this;
    }

    public String getWifiName() {
        return wifiName;
    }

    public SysDevice setWifiName(String wifiName) {
        this.wifiName = wifiName;
        return this;
    }

    public String getIp() {
        return ip;
    }

    public SysDevice setIp(String ip) {
        this.ip = ip;
        return this;
    }

    public String getChipModelName() {
        return chipModelName;
    }

    public SysDevice setChipModelName(String chipModelName) {
        this.chipModelName = chipModelName;
        return this;
    }

    public String getVersion() {
        return version;
    }

    public SysDevice setVersion(String version) {
        this.version = version;
        return this;
    }

    @Override
    public String toString() {
        return "SysDevice [deviceId=" + deviceId + ", sessionId=" + sessionId + ", configId=" + configId
                + ", configName=" + configName + ", configDesc=" + configDesc + ", deviceName=" + deviceName
                + ", state=" + state + ", ip=" + ip
                + ", totalMessage=" + totalMessage + ", code=" + code + ", audioPath=" + audioPath + ", lastLogin="
                + lastLogin + ", wifiName=" + wifiName + ", chipModelName=" + chipModelName + ", version="
                + version + "]";
    }
}