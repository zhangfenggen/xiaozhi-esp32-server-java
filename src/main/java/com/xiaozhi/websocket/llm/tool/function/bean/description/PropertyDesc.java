package com.xiaozhi.websocket.llm.tool.function.bean.description;

import com.xiaozhi.utils.JsonUtil;

/**
 * function_call的参数定义
 */
public class PropertyDesc {
    private String type = "string";
    private String description;

    public PropertyDesc(String description) {
        this.description = description;
    }

    public PropertyDesc(String type, String description) {
        this.type = type;
        this.description = description;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    @Override
    public String toString() {
        return JsonUtil.toJson(this);
    }
}
