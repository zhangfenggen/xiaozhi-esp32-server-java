package com.xiaozhi.entity;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/**
 * 设备表
 * 
 * @author Joey
 * 
 */
@JsonIgnoreProperties({ "startTime", "endTime", "start", "limit", "userId" })
public class SysDevice extends Base {

    private Integer deviceId;

    /**
     * 设备名称
     */
    private String deviceName;

    /**
     * 设备状态
     */
    private Integer state;

    /**
     * 设备对话次数
     */
    private Integer totalMessage;

    /**
     * 建立人ID
     */
    private Integer principalId;

    public Integer getdeviceId() {
        return deviceId;
    }

    public void setdeviceId(Integer deviceId) {
        this.deviceId = deviceId;
    }

    public String getdeviceName() {
        return deviceName;
    }

    public void setdeviceName(String deviceName) {
        this.deviceName = deviceName;
    }

    public Integer getState() {
        return state;
    }

    public void setState(Integer state) {
        this.state = state;
    }

    public Integer getTotalMessage() {
        return totalMessage;
    }

    public void setTotalMessage(Integer totalMessage) {
        this.totalMessage = totalMessage;
    }

    public Integer getPrincipalId() {
        return principalId;
    }

    public void setPrincipalId(Integer principalId) {
        this.principalId = principalId;
    }

}