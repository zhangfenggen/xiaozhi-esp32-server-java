package com.xiaozhi.websocket.llm.tool.function.bean.description;

import com.xiaozhi.utils.JsonUtil;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * function_call的参数定义
 */
public class ParameterDesc {
    private String type = "object";
    private Map<String, PropertyDesc> properties = new HashMap<>();
    private List<String> required = new ArrayList<>();

    public ParameterDesc() {
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public Map<String, PropertyDesc> getProperties() {
        return properties;
    }

    public void setProperties(Map<String, PropertyDesc> properties) {
        this.properties = properties;
    }

    public List<String> getRequired() {
        return required;
    }

    public void setRequired(List<String> required) {
        this.required = required;
    }

    public ParameterDesc addProperty(String name, PropertyDesc property, boolean required) {
        this.properties.put(name, property);
        if (required) {
            this.required.add(name);
        }
        return this;
    }

    @Override
    public String toString() {
        return JsonUtil.toJson(this);
    }
}
