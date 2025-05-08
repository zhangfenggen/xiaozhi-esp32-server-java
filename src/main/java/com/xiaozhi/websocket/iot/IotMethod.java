package com.xiaozhi.websocket.iot;

import com.xiaozhi.utils.JsonUtil;

import java.util.Map;

/**
 * function_call的方法定义
 */
public class IotMethod {
    /**
     * 方法名称
     */
    private String name;
    /**
     * 方法描述
     */
    private String description;
    /**
     * 方法参数
     */
    private Map<String, IotMethodParameter> parameters;

    public IotMethod(String name, String description,
                     Map<String, IotMethodParameter> parameters) {
        this.name = name;
        this.description = description;
        this.parameters = parameters;
    }

    public String getName() {
        return name;
    }

    public String getDescription() {
        return description;
    }

    public Map<String, IotMethodParameter> getParameters() {
        return parameters;
    }

    @Override
    public String toString() {
        return JsonUtil.toJson(this);
    }
}
