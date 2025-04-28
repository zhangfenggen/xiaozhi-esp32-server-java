package com.xiaozhi.entity;

import java.util.Date;

/**
 * 提示词模板实体类
 */
public class SysTemplate extends Base {
    
    private Integer templateId;     // 模板ID
    private String templateName;    // 模板名称
    private String templateDesc;    // 模板描述
    private String templateContent; // 模板内容
    private String category;        // 模板分类
    private String isDefault;      // 是否默认模板(1是 0否)
    private String state;          // 状态(1启用 0禁用)
    private Date updateTime;        // 更新时间
    
    public Integer getTemplateId() {
        return templateId;
    }
    
    public SysTemplate setTemplateId(Integer templateId) {
        this.templateId = templateId;
        return this;
    }
    
    public String getTemplateName() {
        return templateName;
    }
    
    public SysTemplate setTemplateName(String templateName) {
        this.templateName = templateName;
        return this;
    }
    
    public String getTemplateDesc() {
        return templateDesc;
    }
    
    public SysTemplate setTemplateDesc(String templateDesc) {
        this.templateDesc = templateDesc;
        return this;
    }
    
    public String getTemplateContent() {
        return templateContent;
    }
    
    public SysTemplate setTemplateContent(String templateContent) {
        this.templateContent = templateContent;
        return this;
    }
    
    public String getCategory() {
        return category;
    }
    
    public SysTemplate setCategory(String category) {
        this.category = category;
        return this;
    }
    
    public String getIsDefault() {
        return isDefault;
    }
    
    public SysTemplate setIsDefault(String isDefault) {
        this.isDefault = isDefault;
        return this;
    }
    
    public String getState() {
        return state;
    }
    
    public SysTemplate setState(String state) {
        this.state = state;
        return this;
    }
    
    public Date getUpdateTime() {
        return updateTime;
    }
    
    public SysTemplate setUpdateTime(Date updateTime) {
        this.updateTime = updateTime;
        return this;
    }
    
    @Override
    public String toString() {
        return "SysTemplate{" +
                "templateId=" + templateId +
                ", templateName='" + templateName + '\'' +
                ", templateDesc='" + templateDesc + '\'' +
                ", templateContent='" + templateContent + '\'' +
                ", category='" + category + '\'' +
                ", isDefault=" + isDefault +
                ", state=" + state +
                ", updateTime=" + updateTime +
                '}';
    }
}