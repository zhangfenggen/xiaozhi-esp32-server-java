package com.xiaozhi.websocket.llm.tool.function.bean.description;

import com.xiaozhi.utils.JsonUtil;

/**
 * function_call的函数定义
 */
public class FunctionDesc {
    /**
     * 函数名称
     */
    private String name;
    /**
     * 函数描述
     */
    private String description;
    /**
     * 函数参数
     */
    private ParameterDesc parameters;

    public FunctionDesc(String name, String description) {
        this.name = name;
        this.description = description;
    }

    public FunctionDesc(String name, String description, ParameterDesc parameters) {
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

    public ParameterDesc getParameters() {
        return parameters;
    }

    public void setParameters(ParameterDesc parameters) {
        this.parameters = parameters;
    }

    @Override
    public String toString() {
        return JsonUtil.toJson(this);
    }
}
