package com.xiaozhi.websocket.llm.providers;

import com.fasterxml.jackson.core.type.TypeReference;
import com.xiaozhi.websocket.llm.api.AbstractLlmService;
import com.xiaozhi.websocket.llm.api.StreamResponseListener;
import com.xiaozhi.websocket.llm.api.ToolCallInfo;
import com.xiaozhi.websocket.llm.memory.ModelContext;
import com.xiaozhi.websocket.llm.tool.ToolResponse;
import okhttp3.*;
import okio.BufferedSource;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Ollama LLM服务实现
 */
public class OllamaService extends AbstractLlmService {

    /**
     * 构造函数
     * 
     * @param endpoint API端点
     * @param apiKey   API密钥
     * @param model    模型名称
     */
    public OllamaService(String endpoint, String appId, String apiKey, String apiSecret, String model) {
        super(endpoint, appId, apiKey, apiSecret, model);
    }

    @Override
    protected String chat(List<Map<String, Object>> messages) throws IOException {
        // 构建请求体
        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("model", model);
        requestBody.put("messages", messages);

        // 转换为JSON
        String jsonBody = objectMapper.writeValueAsString(requestBody);

        // 构建请求
        Request request = new Request.Builder()
                .url(endpoint + "/api/chat")
                .post(RequestBody.create(jsonBody, JSON))
                .build();

        // 发送请求
        try (Response response = client.newCall(request).execute()) {
            if (!response.isSuccessful()) {
                throw new IOException("请求失败: " + response);
            }

            String responseBody = response.body().string();
            Map<String, Object> responseMap = objectMapper.readValue(responseBody,
                    new TypeReference<Map<String, Object>>() {
                    });

            Map<String, Object> message = (Map<String, Object>) responseMap.get("message");
            if (message != null) {
                return (String) message.get("content");
            }

            throw new IOException("无法解析Ollama响应");
        }
    }

    @Override
    protected void chatStream(List<Map<String, Object>> messages, StreamResponseListener streamListener, ModelContext modelContext)
            throws IOException {
        // 构建请求体
        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("model", model);
        requestBody.put("stream", true);
        requestBody.put("messages", messages);

        // 转换为JSON
        String jsonBody = objectMapper.writeValueAsString(requestBody);

        // 构建请求
        Request request = new Request.Builder()
                .url(endpoint + "/api/chat")
                .post(RequestBody.create(jsonBody, JSON))
                .build();

        // 通知开始
        streamListener.onStart();

        // 发送请求
        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                logger.error("流式请求失败: {}", e.getMessage(), e);
                streamListener.onError(e);
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                if (!response.isSuccessful()) {
                    String errorMsg = "流式请求响应失败: " + response;
                    logger.error(errorMsg);
                    streamListener.onError(new IOException(errorMsg));
                    return;
                }

                try (ResponseBody responseBody = response.body()) {
                    if (responseBody == null) {
                        String errorMsg = "响应体为空";
                        logger.error(errorMsg);
                        streamListener.onError(new IOException(errorMsg));
                        return;
                    }

                    BufferedSource source = responseBody.source();
                    StringBuilder fullResponse = new StringBuilder();

                    while (!source.exhausted()) {
                        String line = source.readUtf8Line();
                        if (line == null) {
                            break;
                        }
                        if (line.isEmpty()) {
                            continue;
                        }
                        try {
                            Map<String, Object> data = objectMapper.readValue(line,
                                    new TypeReference<Map<String, Object>>() {
                                    });
                            if (data.containsKey("message")) {
                                Map<String, Object> message = (Map<String, Object>) data.get("message");
                                if (message != null && message.containsKey("content")) {
                                    String content = (String) message.get("content");
                                    if (content != null && !content.isEmpty()) {
                                        streamListener.onToken(content);
                                        fullResponse.append(content);
                                    }
                                }
                            }
                        } catch (Exception e) {
                            logger.error("解析流式响应失败: {}", e.getMessage(), e);
                            streamListener.onError(e);
                        }
                    }

                    // 通知完成
                    streamListener.onComplete(fullResponse.toString());
                    streamListener.onFinal(messages, OllamaService.this);
                }
            }
        });
    }

    @Override
    public String getProviderName() {
        return "ollama";
    }
}