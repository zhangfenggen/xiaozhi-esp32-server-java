package com.xiaozhi.llm;

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
import com.xiaozhi.entity.SysModel;
import com.xiaozhi.service.SysModelService;

/**
 * 模型管理
 */
@Service
public class LlmManager {

    // 定义标点符号
    private static final String PUNCTUATIONS = "，。？?!！";

    // 最小文本长度阈值
    private static final int MIN_TEXT_LENGTH = 5;

    @Autowired
    private SysModelService modelConfigService;

    // LLM 实例缓存
    private Map<String, Map<Integer, Llm>> deviceLlmInstances = new ConcurrentHashMap<>();

    /**
     * 文本处理结果类，包含分割后的文本和相关信息
     */
    public static class TextSegmentResult {
        private String content; // 分割后的文本内容
        private boolean isFirstText; // 是否为第一段文本
        private boolean isLastText; // 是否为最后一段文本
        private boolean hasContent; // 是否有内容需要处理
        private StringBuilder newBuffer; // 处理后的缓冲区

        public TextSegmentResult(String content, boolean isFirstText, boolean isLastText,
                boolean hasContent, StringBuilder newBuffer) {
            this.content = content;
            this.isFirstText = isFirstText;
            this.isLastText = isLastText;
            this.hasContent = hasContent;
            this.newBuffer = newBuffer;
        }

        public String getContent() {
            return content;
        }

        public boolean isFirstText() {
            return isFirstText;
        }

        public boolean isLastText() {
            return isLastText;
        }

        public boolean hasContent() {
            return hasContent;
        }

        public StringBuilder getNewBuffer() {
            return newBuffer;
        }
    }

    // 获取指定设备的 LLM 实例
    public Llm getLlm(String deviceId, Integer modelId) {
        return deviceLlmInstances
                .computeIfAbsent(deviceId, k -> new ConcurrentHashMap<>())
                .computeIfAbsent(modelId, k -> createLlmInstance(modelId));
    }

    // 创建 LLM 实例
    private Llm createLlmInstance(Integer modelId) {
        // 根据模型ID查询模型配置
        SysModel config = modelConfigService.selectModelByModelId(modelId);
        switch (config.getType().toLowerCase()) {
            case "openai":
                OpenAILlmConfig openAIConfig = new OpenAILlmConfig();
                openAIConfig.setApiKey(config.getApiKey());
                return new OpenAILlm(openAIConfig);

            case "qwen":
                QwenLlmConfig qwenConfig = new QwenLlmConfig();
                qwenConfig.setApiKey(config.getApiKey());
                qwenConfig.setModel(config.getModelName());
                return new QwenLlm(qwenConfig);

            case "spark":
                SparkLlmConfig sparkConfig = new SparkLlmConfig();
                sparkConfig.setAppId(config.getAppId());
                sparkConfig.setApiKey(config.getApiKey());
                sparkConfig.setApiSecret(config.getApiSecret());
                return new SparkLlm(sparkConfig);

            default:
                OllamaLlmConfig ollamaConfig = new OllamaLlmConfig();
                ollamaConfig.setModel(config.getModelName());
                ollamaConfig.setEndpoint(config.getApiUrl());
                return new OllamaLlm(ollamaConfig);
        }

    }

    public TextSegmentResult processTextSegment(StringBuilder buffer, String newContent,
            boolean isFirstText, boolean isLastResponse) {

        // 创建新缓冲区并添加新内容
        StringBuilder newBuffer = new StringBuilder(buffer);
        if (newContent != null) {
            newBuffer.append(newContent);
        }

        // 查找第一个标点符号
        int firstPunctuationIndex = findFirstPunctuation(newBuffer);

        // 情况1: 找到标点符号
        if (firstPunctuationIndex != -1) {
            return handleFoundPunctuation(newBuffer, firstPunctuationIndex,
                    isFirstText, isLastResponse);
        }

        // 情况2: 最后响应且缓冲区有内容
        if (isLastResponse && newBuffer.length() > 0) {
            String remainingSentence = newBuffer.toString().trim();
            if (!remainingSentence.isEmpty()) {
                newBuffer.setLength(0);
                return new TextSegmentResult(remainingSentence,
                        isFirstText, true, true, newBuffer);
            }
        }

        // 情况3: 默认情况，继续等待
        return new TextSegmentResult("", isFirstText,
                isLastResponse, false, newBuffer);
    }

    // 抽取处理找到标点符号的逻辑
    private TextSegmentResult handleFoundPunctuation(StringBuilder newBuffer, int firstPunctuationIndex,
            boolean isFirstText, boolean isLastResponse) {

        // 提取第一个句子
        String firstSentence = newBuffer.substring(0, firstPunctuationIndex + 1);

        // 非第一段文本，直接处理
        if (!isFirstText) {
            newBuffer.delete(0, firstPunctuationIndex + 1);
            return new TextSegmentResult(firstSentence,
                    false, isLastResponse, true, newBuffer);
        }

        // 第一段文本，检查长度
        if (firstSentence.length() >= MIN_TEXT_LENGTH || isLastResponse) {
            newBuffer.delete(0, firstPunctuationIndex + 1);
            return new TextSegmentResult(firstSentence,
                    true, isLastResponse, true, newBuffer);
        }

        // 第一句话长度不足，尝试查找第二个标点符号
        return handleShortFirstSentence(newBuffer, firstPunctuationIndex, firstSentence,
                isLastResponse);
    }

    // 处理第一句话长度不足的情况
    private TextSegmentResult handleShortFirstSentence(StringBuilder newBuffer, int firstPunctuationIndex,
            String firstSentence, boolean isLastResponse) {

        String remainingText = newBuffer.substring(firstPunctuationIndex + 1);
        int secondPunctuationIndex = findFirstPunctuation(new StringBuilder(remainingText));

        // 找到第二个标点符号，合并两个句子
        if (secondPunctuationIndex != -1) {
            int actualSecondIndex = firstPunctuationIndex + 1 + secondPunctuationIndex;
            String combinedSentence = newBuffer.substring(0, actualSecondIndex + 1);
            newBuffer.delete(0, actualSecondIndex + 1);
            return new TextSegmentResult(combinedSentence,
                    true, isLastResponse, true, newBuffer);
        }

        // 检查后面是否有任何标点符号
        boolean hasMorePunctuation = containsAnyPunctuation(remainingText);
        if (hasMorePunctuation) {
            newBuffer.delete(0, firstPunctuationIndex + 1);
            return new TextSegmentResult(firstSentence,
                    true, isLastResponse, true, newBuffer);
        }

        // 继续等待
        return new TextSegmentResult("", true,
                isLastResponse, false, newBuffer);
    }

    // 检查字符串中是否包含任何标点符号
    private boolean containsAnyPunctuation(String text) {
        for (int i = 0; i < text.length(); i++) {
            if (PUNCTUATIONS.indexOf(text.charAt(i)) != -1) {
                return true;
            }
        }
        return false;
    }

    /**
     * 找到第一个标点符号的位置
     */
    public int findFirstPunctuation(StringBuilder sb) {
        for (int i = 0; i < sb.length(); i++) {
            if (PUNCTUATIONS.indexOf(sb.charAt(i)) != -1) {
                return i;
            }
        }
        return -1;
    }
}