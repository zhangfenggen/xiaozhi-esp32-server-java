package com.xiaozhi.entity;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/**
 * 角色配置
 * 
 * @author Joey
 * 
 */
@JsonIgnoreProperties({ "startTime", "endTime", "start", "limit", "userId", "code" })
public class SysRole extends SysConfig {
    private Integer roleId;

    private String roleName;

    private String roleDesc;

    private String voiceName;

    private String state;

    private Integer ttsId;

    public Integer getTtsId() {
        return ttsId;
    }
    public SysRole setTtsId(Integer ttsId) {
        this.ttsId = ttsId;
        return this;
    }

    public Integer getRoleId() {
        return roleId;
    }

    public SysRole setRoleId(Integer roleId) {
        this.roleId = roleId;
        return this;
    }

    public String getRoleName() {
        return roleName;
    }

    public SysRole setRoleName(String roleName) {
        this.roleName = roleName;
        return this;
    }

    public String getRoleDesc() {
        return roleDesc;
    }

    public SysRole setRoleDesc(String roleDesc) {
        this.roleDesc = roleDesc;
        return this;
    }

    public String getVoiceName() {
        return voiceName;
    }

    public SysRole setVoiceName(String voiceName) {
        this.voiceName = voiceName;
        return this;
    }

    public String getState() {
        return state;
    }

    public SysRole setState(String state) {
        this.state = state;
        return this;
    }
}
