package com.xiaozhi.websocket.service.stt.impl;

import com.xiaozhi.websocket.service.stt.AbstractSttService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

/**
 * 备用STT服务，当其他服务都不可用时使用
 * 这个服务不实际进行语音识别，只返回一个固定的消息
 */
@Service("fallbackSttService")
public class FallbackSttService extends AbstractSttService {

    private static final Logger logger = LoggerFactory.getLogger(FallbackSttService.class);

    public FallbackSttService() {
        this.available = true; // 备用服务始终可用
    }

    @Override
    public boolean initialize() {
        logger.info("初始化备用STT服务");
        return true;
    }

    @Override
    public String processAudio(byte[] audioData) {
        logger.info("使用备用STT服务处理音频");
        saveAudioFile(audioData); // 仍然保存音频文件，以便后续可能的处理
        return "{\"text\":\"无法识别语音，请检查STT服务配置。\"}";
    }

    @Override
    public void cleanup() {
        // 无需清理资源
    }

    @Override
    public String getProviderName() {
        return "Fallback";
    }
}