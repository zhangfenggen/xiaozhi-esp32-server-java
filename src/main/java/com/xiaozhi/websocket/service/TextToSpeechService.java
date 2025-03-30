package com.xiaozhi.websocket.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.xiaozhi.entity.SysConfig;
import com.xiaozhi.service.SysConfigService;
import com.xiaozhi.websocket.tts.TtsService;
import com.xiaozhi.websocket.tts.factory.TtsServiceFactory;

@Service
public class TextToSpeechService {
    private static final Logger logger = LoggerFactory.getLogger(TextToSpeechService.class);

    @Autowired
    private TtsServiceFactory ttsServiceFactory;

    @Autowired
    private SysConfigService configService;
    // 默认语音
    private static final String DEFAULT_VOICE = "zh-CN-XiaoyiNeural";

    /**
     * 使用指定语音将文本转换为语音
     */
    public String textToSpeech(String message, String voiceName) throws Exception {
        return textToSpeech(message, voiceName, 16000, 1);
    }

    /**
     * 使用默认语音，指定采样率和通道数将文本转换为语音
     */
    public String textToSpeech(String message, int sampleRate, int channels) throws Exception {
        return textToSpeech(message, DEFAULT_VOICE, sampleRate, channels);
    }

    /**
     * 将文本转换为语音，生成MP3文件（带自定义参数）
     * 
     * @param message    要转换为语音的文本
     * @param voiceName  语音名称
     * @param sampleRate 采样率
     * @param channels   通道数
     * @return 生成的MP3文件路径
     */
    public String textToSpeech(String message, String voiceName, int sampleRate, int channels) throws Exception {
        // 获取默认TTS服务
        TtsService ttsService = ttsServiceFactory.getDefaultTtsService();
        try {
            // 使用TTS服务将文本转换为语音
            return ttsService.textToSpeech(message, voiceName, sampleRate, channels);
        } catch (Exception e) {
            logger.error("文本转语音失败: {}", e.getMessage(), e);
            throw e;
        }
    }

    /**
     * 根据配置ID获取TTS服务并转换文本为语音
     * 
     * @param message  要转换为语音的文本
     * @param configId 配置ID
     * @return 生成的MP3文件路径
     */
    public String textToSpeechWithConfig(String message, Integer configId) throws Exception {
        if (configId == null) {
            return textToSpeech(message);
        }

        // 获取配置
        SysConfig config = configService.selectConfigById(configId);
        if (config == null) {
            logger.warn("未找到配置ID: {}, 使用默认TTS服务", configId);
            return textToSpeech(message);
        }

        // 获取对应的TTS服务
        TtsService ttsService = ttsServiceFactory.getTtsService(config);

        try {
            // 使用TTS服务将文本转换为语音
            return ttsService.textToSpeech(message);
        } catch (Exception e) {
            logger.error("文本转语音失败: {}", e.getMessage(), e);
            throw e;
        }
    }
}
