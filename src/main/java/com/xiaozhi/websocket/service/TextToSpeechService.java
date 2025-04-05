package com.xiaozhi.websocket.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.xiaozhi.entity.SysConfig;
import com.xiaozhi.websocket.tts.TtsService;
import com.xiaozhi.websocket.tts.factory.TtsServiceFactory;

import java.util.function.Consumer;

@Service
public class TextToSpeechService {
    private static final Logger logger = LoggerFactory.getLogger(TextToSpeechService.class);

    @Autowired
    private TtsServiceFactory ttsServiceFactory;

    public String textToSpeech(String message) throws Exception {
        // 获取默认TTS服务
        TtsService ttsService = ttsServiceFactory.getTtsService();
        try {
            // 使用TTS服务将文本转换为语音
            return ttsService.textToSpeech(message);
        } catch (Exception e) {
            logger.error("文本转语音失败: {}", e.getMessage(), e);
            throw e;
        }
    }

    /**
     * 将文本转换为语音
     * 
     * @param message   要转换为语音的文本
     * @param config    配置信息
     * @param voiceName 语音名称
     * @return 生成的音频文件路径
     */
    public String textToSpeech(String message, SysConfig config, String voiceName) throws Exception {
        // 获取默认TTS服务
        TtsService ttsService = ttsServiceFactory.getTtsService(config, voiceName);
        try {
            // 使用TTS服务将文本转换为语音
            return ttsService.textToSpeech(message);
        } catch (Exception e) {
            logger.error("文本转语音失败: {}", e.getMessage(), e);
            throw e;
        }
    }

    /**
     * 流式将文本转换为语音
     * 
     * @param message           要转换为语音的文本
     * @param audioDataConsumer 音频数据消费者
     * @throws Exception 转换过程中可能发生的异常
     */
    public void streamTextToSpeech(String message, Consumer<byte[]> audioDataConsumer) throws Exception {
        // 获取默认TTS服务
        TtsService ttsService = ttsServiceFactory.getTtsService();
        try {
            // 使用TTS服务流式转换文本为语音
            ttsService.streamTextToSpeech(message, audioDataConsumer);
        } catch (Exception e) {
            logger.error("流式文本转语音失败: {}", e.getMessage(), e);
            throw e;
        }
    }

    /**
     * 流式将文本转换为语音（带配置）
     * 
     * @param message           要转换为语音的文本
     * @param config            配置信息
     * @param voiceName         语音名称
     * @param audioDataConsumer 音频数据消费者
     * @throws Exception 转换过程中可能发生的异常
     */
    public void streamTextToSpeech(String message, SysConfig config, String voiceName,
            Consumer<byte[]> audioDataConsumer) throws Exception {
        // 获取TTS服务
        TtsService ttsService = ttsServiceFactory.getTtsService(config, voiceName);
        try {
            // 使用TTS服务流式转换文本为语音
            ttsService.streamTextToSpeech(message, audioDataConsumer);
        } catch (Exception e) {
            logger.error("流式文本转语音失败: {}", e.getMessage(), e);
            throw e;
        }
    }
}