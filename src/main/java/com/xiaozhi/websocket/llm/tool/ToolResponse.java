package com.xiaozhi.websocket.llm.tool;

import com.xiaozhi.utils.JsonUtil;

/**
 * 工具处理后的返回值
 */
public class ToolResponse {
    /**
     * 工具类型
     */
    private ToolType toolType;
    /**
     * 动作类型
     */
    private ActionType actionType;
    /**
     * 工具处理后的结果；比如音量值：60
     */
    private String result;
    /**
     * 最终的响应；比如：当前扬声器的音量值为xx
     */
    private String response;

    public ToolResponse(ToolType toolType, ActionType actionType, String result, String response) {
        this.toolType = toolType;
        this.actionType = actionType;
        this.result = result;
        this.response = response;
    }

    public ActionType getActionType() {
        return actionType;
    }

    public String getResult() {
        return result;
    }

    public String getResponse() {
        return response;
    }

    @Override
    public String toString() {
        return JsonUtil.toJson(this);
    }
}
