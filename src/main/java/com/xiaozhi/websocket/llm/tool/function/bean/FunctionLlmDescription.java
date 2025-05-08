package com.xiaozhi.websocket.llm.tool.function.bean;

import com.xiaozhi.utils.JsonUtil;
import com.xiaozhi.websocket.llm.tool.function.bean.description.FunctionDesc;

/**
 * function_call的工具类描述结构
 */
public class FunctionLlmDescription {
    private String type = "function";
    private FunctionDesc function;

    public FunctionLlmDescription(FunctionDesc function) {
        this.function = function;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public FunctionDesc getFunction() {
        return function;
    }

    public void setFunction(FunctionDesc function) {
        this.function = function;
    }

    @Override
    public String toString() {
        return JsonUtil.toJson(this);
    }
}
