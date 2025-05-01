package com.xiaozhi.websocket.iot;

import com.xiaozhi.utils.JsonUtil;

/**
 * function_call的参数定义
 */
public class IotMethodParameter {
    /**
     * 参数名称
     */
    private String name;
    /**
     * 参数描述
     */
    private String description;
    /**
     * 参数类型
     */
    private String type;

    public IotMethodParameter(String name, String description, String type) {
        this.name = name;
        this.description = description;
        this.type = type;
    }

    public String getName() {
        return name;
    }

    public String getDescription() {
        return description;
    }

    public String getType() {
        return type;
    }

    @Override
    public String toString() {
        return JsonUtil.toJson(this);
    }
}
