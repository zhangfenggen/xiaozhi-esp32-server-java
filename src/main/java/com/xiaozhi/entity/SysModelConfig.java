package com.xiaozhi.entity;


/**
 * 模型配置
 * 
 * @author Joey
 * 
 */
public class SysModelConfig extends Base {
    private Integer modelId;

    /**
     * 模型名称
     */
    private String modelName;

    private String apiKey;

    private String apiSecret;

    private String apiUrl;

    public Integer getModelId() {
        return modelId;
    }
    public void setModelId(Integer modelId) {
        this.modelId = modelId;
    }
    public String getModelName() {
        return modelName;
    }
    public void setModelName(String modelName) {
        this.modelName = modelName;
    }
    public String getApiKey() {
        return apiKey;
    }
    public void setApiKey(String apiKey) {
        this.apiKey = apiKey;
    }
    public String getApiSecret() {
        return apiSecret;
    }
    public void setApiSecret(String apiSecret) {
        this.apiSecret = apiSecret;
    }
    public String getApiUrl() {
        return apiUrl;
    }
    public void setApiUrl(String apiUrl) {
        this.apiUrl = apiUrl;
    }
}
