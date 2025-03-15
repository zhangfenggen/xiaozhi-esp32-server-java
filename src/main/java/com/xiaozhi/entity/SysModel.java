package com.xiaozhi.entity;

/**
 * 模型配置
 * 
 * @author Joey
 * 
 */
public class SysModel extends Base {
    private Integer modelId;

    /**
     * 模型名称
     */
    private String modelName;

    /**
     * 模型描述
     */
    private String modelDesc;

    /**
     * 模型类型（openai、qwen……）
     */
    private String type;

    private String appId;

    private String apiKey;

    private String apiSecret;

    private String apiUrl;

    private String state;

    public Integer getModelId() {
        return modelId;
    }

    public SysModel setModelId(Integer modelId) {
        this.modelId = modelId;
        return this;
    }

    public String getModelName() {
        return modelName;
    }

    public SysModel setModelName(String modelName) {
        this.modelName = modelName;
        return this;
    }

    public String getModelDesc() {
        return modelDesc;
    }

    public SysModel setModelDesc(String modelDesc) {
        this.modelDesc = modelDesc;
        return this;
    }

    public String getType() {
        return type;
    }

    public SysModel setType(String type) {
        this.type = type;
        return this;
    }

    public String getAppId() {
        return appId;
    }

    public SysModel setAppId(String appId) {
        this.appId = appId;
        return this;
    }

    public String getApiKey() {
        return apiKey;
    }

    public SysModel setApiKey(String apiKey) {
        this.apiKey = apiKey;
        return this;
    }

    public String getApiSecret() {
        return apiSecret;
    }

    public SysModel setApiSecret(String apiSecret) {
        this.apiSecret = apiSecret;
        return this;
    }

    public String getApiUrl() {
        return apiUrl;
    }

    public SysModel setApiUrl(String apiUrl) {
        this.apiUrl = apiUrl;
        return this;
    }

    public String getState() {
        return state;
    }

    public SysModel setState(String state) {
        this.state = state;
        return this;
    }
}
