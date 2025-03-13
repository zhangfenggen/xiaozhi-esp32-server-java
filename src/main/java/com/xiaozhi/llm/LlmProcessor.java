package com.xiaozhi.llm;

import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;

/**
 * 模型对话接口
 */

public interface LlmProcessor {
    /**
     * 对话
     * 
     * @param deviceId       设备 ID
     * @param sessionId      会话 ID
     * @param message        用户输入的消息
     * @param onDataReceived 数据处理回调
     * @return CompletableFuture<Void>，表示异步任务
     */
    CompletableFuture<Void> chat(String deviceId, String sessionId, String message, Consumer<String> onDataReceived);

}