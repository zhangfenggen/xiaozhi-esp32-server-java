package com.xiaozhi.websocket.service;

import com.agentsflex.core.llm.Llm;
import com.agentsflex.core.message.HumanMessage;
import com.agentsflex.core.message.SystemMessage;
import com.agentsflex.core.prompt.HistoriesPrompt;
import com.xiaozhi.entity.SysDevice;
import com.xiaozhi.websocket.llm.DatabaseChatMemory;
import com.xiaozhi.websocket.llm.LlmManager;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.web.socket.WebSocketSession;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * 处理LLM响应和语音合成的服务
 */
@Service
public class LlmResponseService {

    private static final Logger logger = LoggerFactory.getLogger(LlmResponseService.class);

    // 定义标点符号
    private static final String PUNCTUATIONS = "，,。？?!！";

    // 最小文本长度阈值
    private static final int MIN_TEXT_LENGTH = 5;

    // 缓存HistoriesPrompt对象，使用设备ID作为键
    private final ConcurrentHashMap<String, HistoriesPrompt> promptCache = new ConcurrentHashMap<>();

    @Autowired
    private LlmManager llmManager;

    @Autowired
    private TextToSpeechService textToSpeechService;

    @Autowired
    private AudioService audioService;

    @Autowired
    @Qualifier("WebSocketMessageService")
    private MessageService messageService;

    @Autowired
    private ApplicationContext applicationContext;

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

    /**
     * 处理用户输入并生成LLM响应
     * 
     * @param session   WebSocket会话
     * @param device    设备信息
     * @param userInput 用户输入文本
     */
    public void processUserInput(WebSocketSession session, SysDevice device, String userInput) {
        String sessionId = session.getId();
        String deviceId = device.getDeviceId();

        // 通知客户端停止监听
        messageService.sendMessage(session, "stt", "stop", userInput);

        try {
            // 获取适合设备的LLM模型
            Llm llm = llmManager.getLlm(deviceId, device.getModelId());

            // 获取或创建HistoriesPrompt
            HistoriesPrompt prompt = getOrCreatePrompt(device);

            prompt.setSystemMessage(new SystemMessage(device.getModelDesc()));
            // 添加用户消息
            prompt.addMessage(new HumanMessage(userInput));
            // 创建文本缓冲区和第一次文本标记
            StringBuilder buffer = new StringBuilder();
            final AtomicBoolean isFirstTextSent = new AtomicBoolean(false);

            // 跟踪最后一个响应的状态
            final AtomicBoolean receivedLastResponse = new AtomicBoolean(false);

            // 跟踪最后一个文本段的索引
            final AtomicBoolean isLastSegmentSent = new AtomicBoolean(false);

            llm.chatStream(prompt, (context, response) -> {
                try {
                    // 获取新生成的文本内容
                    String message = response.getMessage().getContent();

                    // 检查是否是最后一个响应
                    boolean isLastText = response.getMessage().getStatus().toString().equals("END");
                    // 如果是最后一个响应，设置标志
                    if (isLastText) {
                        receivedLastResponse.set(true);
                    }

                    // 使用LlmManager处理文本分割
                    TextSegmentResult result = processTextSegment(
                            buffer,
                            message,
                            !isFirstTextSent.get(),
                            isLastText);

                    // 更新缓冲区
                    buffer.setLength(0);
                    buffer.append(result.getNewBuffer());

                    // 如果有可处理的文本内容
                    if (result.hasContent() && !result.getContent().isEmpty()) {
                        String sentence = result.getContent();

                        // 确保文本内容不为空
                        if (!sentence.isEmpty()) {
                            String audioFilePath = textToSpeechService.textToSpeech(sentence);

                            // 如果是最后一个响应且缓冲区为空，标记这是最后一个文本段
                            boolean isReallyLastText = result.isLastText() ||
                                    (isLastText && buffer.length() == 0 && !isLastSegmentSent.get());

                            if (isLastText && buffer.length() == 0) {
                                isLastSegmentSent.set(true);
                            }

                            // 发送音频，确保最后一个文本段的isLastText为true
                            audioService.sendAudio(session, audioFilePath, sentence, result.isFirstText(),
                                    isReallyLastText);

                            // 如果这是第一个文本块，标记为已发送
                            if (result.isFirstText()) {
                                isFirstTextSent.set(true);
                            }
                        }
                    }

                    // 如果是最后一个响应，完成处理
                    if (isLastText) {

                        String remainingSentence = buffer.toString().trim();
                        // 这里确保最后一段文本的isLastText标志为true
                        boolean isFirst = !isFirstTextSent.get();
                        if (!remainingSentence.isEmpty()) {

                            String audioFilePath = textToSpeechService.textToSpeech(remainingSentence);

                            // 标记这是最后一个文本段
                            isLastSegmentSent.set(true);

                            audioService.sendAudio(session, audioFilePath, remainingSentence,
                                    isFirst, true);
                        }
                    }
                } catch (Exception e) {
                    logger.error("语音合成失败 - SessionId: {}", sessionId, e);
                }
            });

            prompt.getMemory().getMessages().forEach(message -> {
                logger.debug("HistoriesPrompt消息 - SessionId: {}: {}", sessionId, message.getMessageContent());
            });

        } catch (Exception e) {
            logger.error("处理用户输入失败 - SessionId: {}, 错误: {}", sessionId, e.getMessage(), e);
        }
    }

    /**
     * 获取或创建设备对应的HistoriesPrompt对象
     * 
     * @param device 设备信息
     * @return HistoriesPrompt对象
     */
    private HistoriesPrompt getOrCreatePrompt(SysDevice device) {
        String deviceId = device.getDeviceId();

        // 尝试从缓存获取
        return promptCache.computeIfAbsent(deviceId, id -> {
            // 如果缓存中不存在，创建新的HistoriesPrompt
            DatabaseChatMemory chatMemory = applicationContext.getBean(DatabaseChatMemory.class);
            chatMemory.setDevice(device);
            // HistoriesPrompt prompt = new HistoriesPrompt(chatMemory);
            HistoriesPrompt prompt = new HistoriesPrompt();
            prompt.setSystemMessage(new SystemMessage(device.getRoleDesc()));
            logger.info("为设备 {} 创建新的HistoriesPrompt", deviceId);
            return prompt;
        });
    }

    /**
     * 清除特定设备的HistoriesPrompt缓存
     * 
     * @param deviceId 设备ID
     */
    public void clearPromptCache(String deviceId) {
        promptCache.remove(deviceId);
        logger.info("已清除设备 {} 的HistoriesPrompt缓存", deviceId);
    }

    /**
     * 清除所有设备的HistoriesPrompt缓存
     */
    public void clearAllPromptCache() {
        promptCache.clear();
        logger.info("已清除所有设备的HistoriesPrompt缓存");
    }

    public TextSegmentResult processTextSegment(StringBuilder buffer, String newContent,
            boolean isFirstText, boolean isLastResponse) {

        // 创建新缓冲区并添加新内容
        StringBuilder newBuffer = new StringBuilder(buffer);
        if (newContent != null) {
            newBuffer.append(newContent);
        }

        // 如果是最后一个响应但缓冲区为空，创建一个特殊的结果对象
        if (isLastResponse && newBuffer.length() == 0) {
            // 返回一个空内容但标记为最后一个文本的结果，并设置hasContent为true
            return new TextSegmentResult("", isFirstText, true, true, newBuffer);
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
        // 如果是最后一个响应，即使没有内容也设置hasContent为true
        boolean hasContent = isLastResponse;
        return new TextSegmentResult("", isFirstText,
                isLastResponse, hasContent, newBuffer);
    }

    // 抽取处理找到标点符号的逻辑
    private TextSegmentResult handleFoundPunctuation(StringBuilder newBuffer, int firstPunctuationIndex,
            boolean isFirstText, boolean isLastResponse) {

        // 提取第一个句子
        String firstSentence = newBuffer.substring(0, firstPunctuationIndex + 1);
        // 检查标点符号后一个字符是否为结尾
        boolean isEndAfterPunctuation = isEndAfterPunctuation(newBuffer, firstPunctuationIndex);

        // 如果标点符号后是结尾，将其与后续内容合并
        if (isEndAfterPunctuation) {
            // 如果是最后一个响应，直接返回整段内容
            if (isLastResponse) {
                String combinedSentence = newBuffer.toString().trim();
                newBuffer.setLength(0);
                return new TextSegmentResult(combinedSentence, isFirstText, true, true, newBuffer);
            }
            // 否则继续等待更多内容
            return new TextSegmentResult("", isFirstText, isLastResponse, false, newBuffer);
        }

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

    // 检查标点符号后一个字符是否为结尾
    private boolean isEndAfterPunctuation(StringBuilder newBuffer, int punctuationIndex) {
        // 如果标点符号是最后一个字符，直接返回true
        if (punctuationIndex == newBuffer.length() - 1) {
            return true;
        }
        // 检查标点符号后一个字符是否为空白或换行
        char nextChar = newBuffer.charAt(punctuationIndex + 1);
        return Character.isWhitespace(nextChar) || nextChar == '\n';
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

        // 如果是最后响应，即使没有找到更多标点符号也返回hasContent=true
        if (isLastResponse) {
            newBuffer.delete(0, firstPunctuationIndex + 1);
            return new TextSegmentResult(firstSentence,
                    true, true, true, newBuffer);
        }

        // 继续等待
        // 如果是最后响应，即使没有内容也设置hasContent为true
        boolean hasContent = isLastResponse;
        return new TextSegmentResult("", true,
                isLastResponse, hasContent, newBuffer);
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