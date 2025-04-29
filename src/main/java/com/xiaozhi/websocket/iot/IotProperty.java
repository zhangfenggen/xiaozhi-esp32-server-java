package com.xiaozhi.websocket.iot;

import com.xiaozhi.utils.JsonUtil;

/**
 * function_call的参数定义
 */
public class IotProperty {
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
    /**
     * 参数值
     */
    private Object value;

    public IotProperty(String name, String description, String type) {
        this.name = name;
        this.description = description;
        this.type = type;
    }

    public IotProperty(String name, String description, String type, Object value) {
        this.name = name;
        this.description = description;
        this.type = type;
        this.value = value;
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

    public Object getValue() {
        return value;
    }

    public void setValue(Object value) {
        this.value = value;
    }

    @Override
    public String toString() {
        return JsonUtil.toJson(this);
    }
}
