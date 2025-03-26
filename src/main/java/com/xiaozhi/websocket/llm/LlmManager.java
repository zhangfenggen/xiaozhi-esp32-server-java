package com.xiaozhi.websocket.llm;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.agentsflex.core.llm.Llm;
import com.agentsflex.llm.ollama.OllamaLlm;
import com.agentsflex.llm.ollama.OllamaLlmConfig;
import com.agentsflex.llm.openai.OpenAILlm;
import com.agentsflex.llm.openai.OpenAILlmConfig;
import com.agentsflex.llm.qwen.QwenLlm;
import com.agentsflex.llm.qwen.QwenLlmConfig;
import com.agentsflex.llm.spark.SparkLlm;
import com.agentsflex.llm.spark.SparkLlmConfig;
import com.xiaozhi.entity.SysConfig;
import com.xiaozhi.service.SysConfigService;

/**
 * 模型管理
 */
@Service
public class LlmManager {

    @Autowired
    private SysConfigService configService;

    // LLM 实例缓存
    private Map<String, Map<Integer, Llm>> deviceLlmInstances = new ConcurrentHashMap<>();

    /**
     * 文本处理结果类，包含分割后的文本和相关信息
     */

    // 获取指定设备的 LLM 实例
    public Llm getLlm(String deviceId, Integer configId) {
        return deviceLlmInstances
                .computeIfAbsent(deviceId, k -> new ConcurrentHashMap<>())
                .computeIfAbsent(configId, k -> createLlmInstance(configId));
    }

    // 创建 LLM 实例
    private Llm createLlmInstance(Integer configId) {
        // 根据模型ID查询模型配置
        SysConfig config = configService.selectConfigById(configId);
        switch (config.getProvider().toLowerCase()) {
            case "openai":
                OpenAILlmConfig openAIConfig = new OpenAILlmConfig();
                openAIConfig.setApiKey(config.getApiKey());
                return new OpenAILlm(openAIConfig);

            case "qwen":
                QwenLlmConfig qwenConfig = new QwenLlmConfig();
                qwenConfig.setApiKey(config.getApiKey());
                qwenConfig.setModel(config.getConfigName());
                return new QwenLlm(qwenConfig);

            case "spark":
                SparkLlmConfig sparkConfig = new SparkLlmConfig();
                sparkConfig.setAppId(config.getAppId());
                sparkConfig.setApiKey(config.getApiKey());
                sparkConfig.setApiSecret(config.getApiSecret());
                return new SparkLlm(sparkConfig);

            default:
                OllamaLlmConfig ollamaConfig = new OllamaLlmConfig();
                ollamaConfig.setModel(config.getConfigName());
                ollamaConfig.setEndpoint(config.getApiUrl());
                return new OllamaLlm(ollamaConfig);
        }
    }

}