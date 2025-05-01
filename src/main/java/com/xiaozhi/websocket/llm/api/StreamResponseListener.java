package com.xiaozhi.websocket.llm.api;

import java.util.List;
import java.util.Map;

/**
 * 流式响应监听器接口
 * 用于处理LLM的流式响应
 */
public interface StreamResponseListener {
    
    /**
     * 当流式响应开始时调用
     */
    void onStart();
    
    /**
     * 当接收到新的token时调用
     * 
     * @param token 接收到的token
     */
    void onToken(String token);
    
    /**
     * 当流式响应完成时调用
     * 
     * @param fullResponse 完整的响应内容
     */
    void onComplete(String fullResponse);


    /**
     * 当流式响应完成时调用
     *
     * @param allMessages 当前交互的所有消息列表内容
     * @param llmService 当前处理消息的llmService
     * @param toolCallInfo 如果对话触发了工具调用，则会传入toolCallInfo
     */
    default void onFinal(List<Map<String, Object>> allMessages, LlmService llmService, ToolCallInfo toolCallInfo) {
        //do nothing
    }

    /**
     * 当发生错误时调用
     * 
     * @param e 发生的异常
     */
    void onError(Throwable e);
}