package com.xiaozhi.utils.audio;

/**
 * 语音活动检测器接口
 */
public interface VadDetector {
    /**
     * 初始化会话
     */
    void initializeSession(String sessionId);
    
    /**
     * 处理音频数据
     * @param sessionId 会话ID
     * @param pcmData PCM格式的音频数据
     * @return 如果检测到语音结束，返回完整的音频数据；否则返回null
     */
    byte[] processAudio(String sessionId, byte[] pcmData);
    
    /**
     * 重置会话状态
     */
    void resetSession(String sessionId);
    
    /**
     * 获取当前是否在说话状态
     */
    boolean isSpeaking(String sessionId);
    
    /**
     * 获取当前语音概率
     */
    float getCurrentSpeechProbability(String sessionId);
    
    /**
     * 设置VAD阈值
     */
    void setThreshold(float threshold);
}