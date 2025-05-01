package com.xiaozhi.websocket.llm.tool;

/**
 * function调用后的动作
 */
public enum ActionType {
    ERROR(-1, "错误"),
    NOTFOUND(0, "没有找到函数"),
    NONE(1, "啥也不干"),
    RESPONSE(2, "直接回复"),
    REQLLM(3, "调用函数后再请求llm生成回复");

    private final int code;
    private final String desc;

    ActionType(int code, String desc) {
        this.code = code;
        this.desc = desc;
    }

    public int getCode() {
        return code;
    }

    public String getDesc() {
        return desc;
    }

}
