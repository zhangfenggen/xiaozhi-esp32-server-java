package com.xiaozhi.websocket.llm.api;

import com.fasterxml.jackson.core.type.TypeReference;
import com.xiaozhi.utils.JsonUtil;

import java.util.Collections;
import java.util.Map;

/**
 * 工具处理后的返回值
 */
public class ToolCallInfo {
    public static final String TOOL_CALL_TYPE_FUNCTION_CALL = "FUNCTION_CALL";
    public static final String TOOL_CALL_TYPE_MCP = "MCP";
    /**
     * 工具类型
     */
    protected String type = TOOL_CALL_TYPE_FUNCTION_CALL;
    /**
     * 工具名称
     */
    protected String name;
    /**
     * 工具调用id
     */
    protected String tool_call_id;
    /**
     * 工具调用参数
     */
    protected Map<String, Object> arguments = Collections.EMPTY_MAP;
    /**
     * 工具调用参数json串
     */
    protected StringBuilder argumentsJson = new StringBuilder();

    public ToolCallInfo() {
    }

    public ToolCallInfo(String tool_call_id, String name) {
        this.tool_call_id = tool_call_id;
        this.name = name;
    }

    public ToolCallInfo(String tool_call_id, String name, Map<String, Object> arguments) {
        this.tool_call_id = tool_call_id;
        this.name = name;
        this.arguments = arguments;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getTool_call_id() {
        return tool_call_id;
    }

    public void setTool_call_id(String tool_call_id) {
        this.tool_call_id = tool_call_id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Map<String, Object> getArguments() {
        if(argumentsJson.length() != 0){
            buildArguments();
        }
        return arguments;
    }

    public void setArguments(Map<String, Object> arguments) {
        this.arguments = arguments;
    }

    public void appendArgumentsJson(String json) {
        argumentsJson.append(json);
    }

    public void buildArguments() {
        if (argumentsJson.length() != 0) {
            arguments = JsonUtil.fromJson(argumentsJson.toString(), new TypeReference<Map<String, Object>>() {});
        }
    }
    @Override
    public String toString() {
        return JsonUtil.toJson(this);
    }

    public void clearArgumentsJson() {
        argumentsJson = new StringBuilder();
    }
}
