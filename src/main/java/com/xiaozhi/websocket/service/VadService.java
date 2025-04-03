package com.xiaozhi.websocket.service;

import com.xiaozhi.websocket.audio.detector.VadDetector;
import com.xiaozhi.websocket.audio.detector.impl.SileroVadDetector;
import com.xiaozhi.websocket.audio.processor.OpusProcessor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * 语音活动检测服务，集中处理VAD相关功能
 */
@Service
public class VadService {
    private static final Logger logger = LoggerFactory.getLogger(VadService.class);

    @Autowired
    private VadDetector vadDetector;

    @Autowired
    private OpusProcessor opusProcessor;

    /**
     * 初始化VAD会话
     * 
     * @param sessionId 会话ID
     */
    public void initializeSession(String sessionId) {
        vadDetector.initializeSession(sessionId);
        logger.debug("VAD会话已初始化 - SessionId: {}", sessionId);
    }

    /**
     * 处理接收到的音频数据
     * 
     * @param sessionId 会话ID，用于区分不同设备
     * @param opusData  Opus编码的音频数据
     * @return 如果语音结束，返回完整的音频数据；否则返回null
     */
    public byte[] processIncomingAudio(String sessionId, byte[] opusData) {
        try {
            // 1. 解码Opus数据为PCM
            byte[] pcmData = opusProcessor.decodeOpusFrameToPcm(opusData);

            // 2. 使用VAD处理PCM数据
            byte[] completeAudio = vadDetector.processAudio(sessionId, pcmData);

            // 3. 如果检测到语音结束，返回完整的音频数据
            return completeAudio;
        } catch (Exception e) {
            logger.error("处理音频数据时发生错误 - SessionId: {}", sessionId, e);
            return null;
        }
    }

    /**
     * 重置VAD会话状态
     * 
     * @param sessionId 会话ID
     */
    public void resetSession(String sessionId) {
        vadDetector.resetSession(sessionId);
        logger.debug("VAD会话已重置 - SessionId: {}", sessionId);
    }

    /**
     * 检查当前是否正在说话
     * 
     * @param sessionId 会话ID
     * @return 如果当前正在说话返回true，否则返回false
     */
    public boolean isSpeaking(String sessionId) {
        return vadDetector.isSpeaking(sessionId);
    }

    /**
     * 获取当前语音概率
     * 
     * @param sessionId 会话ID
     * @return 当前语音概率值，范围0.0-1.0
     */
    public float getCurrentSpeechProbability(String sessionId) {
        return vadDetector.getCurrentSpeechProbability(sessionId);
    }

    /**
     * 强制结束当前语音会话
     * 
     * @param sessionId 会话ID
     * @return 已收集的音频数据，如果没有数据则返回null
     */
    public byte[] forceEndSession(String sessionId) {
        if (vadDetector instanceof SileroVadDetector) {
            return ((SileroVadDetector) vadDetector)
                    .forceEndSession(sessionId);
        }
        return null;
    }

    /**
     * 设置VAD阈值
     * 
     * @param threshold 新的阈值 (0.0-1.0)
     */
    public void setThreshold(float threshold) {
        vadDetector.setThreshold(threshold);
    }
}