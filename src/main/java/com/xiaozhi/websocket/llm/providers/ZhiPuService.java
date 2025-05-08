package com.xiaozhi.websocket.llm.providers;

import com.xiaozhi.websocket.llm.api.AbstractOpenAiLlmService;
import org.apache.commons.lang3.StringUtils;

import java.io.IOException;
import java.util.List;
import java.util.Map;

/**
 * 智谱 LLM服务实现
 */
public class ZhiPuService extends AbstractOpenAiLlmService {

    /**
     * 构造函数
     *
     * @param endpoint API端点 (host url)
     * @param apiKey   API密钥 (apiKey)
     * @param model    模型名称
     */
    public ZhiPuService(String endpoint, String appId, String apiKey, String apiSecret, String model) {
        super(endpoint, appId, apiKey, apiSecret, model);
    }

    @Override
    protected String chat(List<Map<String, Object>> messages) throws IOException {
        logger.warn("不支持，请使用流式对话");
        return StringUtils.EMPTY;
    }

    @Override
    public String getProviderName() {
        return "zhipu";
    }
}