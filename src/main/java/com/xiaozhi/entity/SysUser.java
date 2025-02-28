package com.xiaozhi.entity;

import java.io.Serializable;
import java.util.Date;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/**
 * 用户表
 * 
 * @author Joey
 * 
 */
@JsonIgnoreProperties({ "password", "startTime", "endTime", "start" })
public class SysUser extends Base implements Serializable {

    /**
     * serialVersionUID
     */
    private static final long serialVersionUID = -3406166342385856305L;

    /**
     * 用户名
     */
    private String username;

    /**
     * 密码
     */
    private String password;

    /**
     * 姓名
     */
    private String name;

    /**
     * 对话次数
     */
    private Integer messageNumber;

    /**
     * 参加人数
     */
    private Integer aliveNumber;

    /**
     * 总设备数
     */
    private Integer totalDevice;

    /**
     * 头像
     */
    private String avatar;

    /**
     * 用户状态 0、被禁用，1、正常使用
     */
    private Integer state;

    /**
     * 用户类型 0、普通管理（拥有标准权限），1、超级管理（拥有所有权限）
     */
    private Integer isAdmin;

    /**
     * 手机号
     */
    private String tel;

    /**
     * 邮箱
     */
    private String email;

    /**
     * 上次登录IP
     */
    private String loginIp;

    /**
     * 上次登录时间
     */
    @JsonFormat(timezone = "GMT+8", pattern = "yyyy-MM-dd HH:mm:ss")
    private Date loginTime;

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getAvatar() {
        return avatar;
    }

    public void setAvatar(String avatar) {
        this.avatar = avatar;
    }

    public Integer getState() {
        return state;
    }

    public void setState(Integer state) {
        this.state = state;
    }

    public Integer getIsAdmin() {
        return isAdmin;
    }

    public void setIsAdmin(Integer isAdmin) {
        this.isAdmin = isAdmin;
    }

    public Integer getMessageNumber() {
        return messageNumber;
    }

    public void setMessageNumber(Integer messageNumber) {
        this.messageNumber = messageNumber;
    }

    public Integer getAliveNumber() {
        return aliveNumber;
    }

    public void setAliveNumber(Integer aliveNumber) {
        this.aliveNumber = aliveNumber;
    }

    public Integer getTotalDevice() {
        return totalDevice;
    }

    public void setTotalDevice(Integer totalDevice) {
        this.totalDevice = totalDevice;
    }

    public String getTel() {
        return tel;
    }

    public void setTel(String tel) {
        this.tel = tel;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getLoginIp() {
        return loginIp;
    }

    public void setLoginIp(String loginIp) {
        this.loginIp = loginIp;
    }

    public Date getLoginTime() {
        return loginTime;
    }

    public void setLoginTime(Date loginTime) {
        this.loginTime = loginTime;
    }

}
