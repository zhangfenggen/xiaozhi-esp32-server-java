package com.xiaozhi.websocket.llm.api;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.xiaozhi.websocket.llm.memory.ModelContext;
import okhttp3.OkHttpClient;
import okhttp3.MediaType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

/**
 * LLM服务抽象类
 * 实现一些通用功能
 */
public abstract class AbstractLlmService implements LlmService {
    protected static final Logger logger = LoggerFactory.getLogger(AbstractLlmService.class);
    protected static final ObjectMapper objectMapper = new ObjectMapper();
    protected static final OkHttpClient client = new OkHttpClient.Builder()
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(60, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS)
            .build();
    
    protected static final MediaType JSON = MediaType.parse("application/json; charset=utf-8");
    
    protected final String endpoint;
    protected final String apiKey;
    protected final String model;
    
    /**
     * 构造函数
     * 
     * @param endpoint API端点
     * @param apiKey API密钥
     * @param model 模型名称
     */
    public AbstractLlmService(String endpoint, String apiKey, String model) {
        this.endpoint = endpoint;
        this.apiKey = apiKey;
        this.model = model;
    }
    
    @Override
    public String chat(String userMessage, ModelContext modelContext) throws IOException {
        // 保存用户消息
        modelContext.addUserMessage(userMessage);
        
        // 调用实际的聊天方法
        String response = performChat(modelContext.getSystemMessage(), userMessage);
        
        // 保存助手消息
        modelContext.addAssistantMessage(response);
        
        return response;
    }
    
    @Override
    public void chatStream(String userMessage, ModelContext modelContext, StreamResponseListener streamListener) throws IOException {
        // 保存用户消息
        modelContext.addUserMessage(userMessage);
        
        // 创建一个包装监听器，在完成时保存响应
        StreamResponseListener wrappedListener = new StreamResponseListener() {
            @Override
            public void onStart() {
                streamListener.onStart();
            }

            @Override
            public void onToken(String token) {
                streamListener.onToken(token);
            }

            @Override
            public void onComplete(String fullResponse) {
                // 保存助手消息
                modelContext.addAssistantMessage(fullResponse);
                streamListener.onComplete(fullResponse);
            }

            @Override
            public void onError(Throwable e) {
                streamListener.onError(e);
            }
        };
        
        // 调用实际的流式聊天方法
        performChatStream(modelContext.getSystemMessage(), userMessage, wrappedListener);
    }
    
    @Override
    public String getModelName() {
        return model;
    }
    
    /**
     * 执行实际的聊天请求
     * 
     * @param systemMessage 系统消息
     * @param userMessage 用户消息
     * @return 模型回复
     * @throws IOException 如果请求失败
     */
    protected abstract String performChat(String systemMessage, String userMessage) throws IOException;
    
    /**
     * 执行实际的流式聊天请求
     * 
     * @param systemMessage 系统消息
     * @param userMessage 用户消息
     * @param streamListener 流式响应监听器
     * @throws IOException 如果请求失败
     */
    protected abstract void performChatStream(String systemMessage, String userMessage, StreamResponseListener streamListener) throws IOException;
}