package com.xiaozhi.websocket.llm.tool.function.bean;

import com.xiaozhi.websocket.llm.memory.ModelContext;
import com.xiaozhi.websocket.llm.tool.ToolResponse;
import com.xiaozhi.websocket.llm.tool.ToolType;

import java.util.Map;
import java.util.function.Function;

/**
 * function_call的工具类
 */
public class FunctionCallTool {
    /**
     * 工具名称
     */
    private String name;
    /**
     * 工具类型
     */
    private ToolType type;
    /**
     * 工具llm描述
     */
    private FunctionLlmDescription functionLlmDescription;
    /**
     * 工具实际调用函数
     */
    private Function<FunctionParams, ToolResponse> function;

    public FunctionCallTool(String name, ToolType type, FunctionLlmDescription functionLlmDescription, Function<FunctionParams, ToolResponse> function) {
        this.name = name;
        this.type = type;
        this.functionLlmDescription = functionLlmDescription;
        this.function = function;
    }

    public String getName() {
        return name;
    }

    public ToolType getType() {
        return type;
    }

    public FunctionLlmDescription getFunctionLlmDescription() {
        return functionLlmDescription;
    }

    public Function<FunctionParams, ToolResponse> getFunction() {
        return function;
    }

    public static class FunctionParams{
        public ModelContext context;
        public Map<String, Object> params;

        public FunctionParams(ModelContext context, Map<String, Object> params) {
            this.context = context;
            this.params = params;
        }
    }
}
