package com.xiaozhi.websocket.llm.providers;

import com.fasterxml.jackson.core.type.TypeReference;
import com.xiaozhi.websocket.llm.api.AbstractOpenAiLlmService;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * OpenAI LLM服务实现
 */
public class OpenAiService extends AbstractOpenAiLlmService {

    /**
     * 构造函数
     * 
     * @param endpoint API端点
     * @param apiKey   API密钥
     * @param model    模型名称
     */
    public OpenAiService(String endpoint, String appId, String apiKey, String apiSecret, String model) {
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
                .url(endpoint + "/chat/completions")
                .post(RequestBody.create(jsonBody, JSON))
                .addHeader("Authorization", "Bearer " + apiKey)
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

            List<Map<String, Object>> choices = (List<Map<String, Object>>) responseMap.get("choices");
            if (choices != null && !choices.isEmpty()) {
                Map<String, Object> message = (Map<String, Object>) choices.get(0).get("message");
                if (message != null) {
                    return (String) message.get("content");
                }
            }
            throw new IOException("无法解析OpenAI响应");
        }
    }

    @Override
    public String getProviderName() {
        return "openai";
    }
}