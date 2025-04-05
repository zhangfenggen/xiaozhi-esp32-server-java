package com.xiaozhi.websocket.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.xiaozhi.utils.OpusProcessor;
import com.xiaozhi.utils.TarsosNoiseReducer;
import com.xiaozhi.websocket.vad.VadModel;
import com.xiaozhi.websocket.vad.VadSessionState;

import javax.annotation.PostConstruct;
import java.util.concurrent.ConcurrentHashMap;

/**
 * VAD服务 - 负责语音活动检测
 */
@Service
public class VadService {
    private static final Logger logger = LoggerFactory.getLogger(VadService.class);

    @Autowired
    private VadModel vadModel;

    @Autowired
    private OpusProcessor opusProcessor;

    @Autowired(required = false)
    private TarsosNoiseReducer tarsosNoiseReducer;

    // 配置参数
    private float speechThreshold = 0.6f;
    private float silenceThreshold = 0.45f; // speechThreshold - 0.15f
    private float energyThreshold = 0.05f;
    private int minSilenceDurationMs = 500;
    private int maxSilenceDurationMs = 1000;
    private boolean enableNoiseReduction = true;

    // 会话状态管理
    private final ConcurrentHashMap<String, VadSessionState> sessionStates = new ConcurrentHashMap<>();

    // 会话锁管理 - 为每个会话提供独立的锁对象
    private final ConcurrentHashMap<String, Object> sessionLocks = new ConcurrentHashMap<>();

    /**
     * 初始化噪声抑制器
     */
    @PostConstruct
    private void initializeNoiseReducer() {
        if (tarsosNoiseReducer != null) {
            tarsosNoiseReducer.setSpectralSubtractionFactor(1.5);
            tarsosNoiseReducer.setNoiseEstimationFrames(10);
            logger.info("TarsosDSP噪声抑制器参数已优化配置");
        }
    }

    /**
     * 获取会话的锁对象
     */
    private Object getSessionLock(String sessionId) {
        return sessionLocks.computeIfAbsent(sessionId, id -> new Object());
    }

    /**
     * 初始化会话
     * 
     * @param sessionId 会话ID
     * @return 如果是新会话返回true，否则返回false
     */
    public boolean initializeSession(String sessionId) {
        Object lock = getSessionLock(sessionId);

        synchronized (lock) {
            if (sessionStates.containsKey(sessionId)) {
                return false;
            }

            sessionStates.put(sessionId, new VadSessionState());
            if (enableNoiseReduction && tarsosNoiseReducer != null) {
                tarsosNoiseReducer.initializeSession(sessionId);
            }

            logger.debug("VAD会话初始化 - SessionId: {}", sessionId);
            return true;
        }
    }

    /**
     * 处理音频数据并检测语音活动
     * 
     * @param sessionId 会话ID
     * @param opusData  Opus编码的音频数据
     * @return VadResult 包含VAD检测结果和处理后的PCM数据
     */
    public VadResult processAudio(String sessionId, byte[] opusData) {
        Object lock = getSessionLock(sessionId);

        synchronized (lock) {
            try {
                // 获取会话状态，如果不存在则创建
                VadSessionState state = sessionStates.get(sessionId);
                if (state == null) {
                    state = new VadSessionState();
                    sessionStates.put(sessionId, state);

                    if (enableNoiseReduction && tarsosNoiseReducer != null) {
                        tarsosNoiseReducer.initializeSession(sessionId);
                    }

                    logger.debug("VAD会话初始化（处理中） - SessionId: {}", sessionId);
                }

                // 解码Opus数据为PCM
                byte[] pcmData = opusProcessor.decodeOpusFrameToPcm(opusData);

                // 应用噪声抑制（如果启用）
                if (enableNoiseReduction) {
                    pcmData = applyNoiseReduction(sessionId, pcmData);
                }

                // 将PCM数据添加到预缓冲区
                state.addToPrebuffer(pcmData);

                // 如果已经在说话状态，则将数据添加到主缓冲区
                if (state.isSpeaking()) {
                    state.addToMainBuffer(pcmData);
                }

                // 分析音频帧
                if (state.hasEnoughDataForAnalysis()) {
                    float[] samples = state.extractSamplesForAnalysis();
                    float currentEnergy = calculateEnergy(samples);
                    state.updateAverageEnergy(currentEnergy);

                    // 使用VAD模型获取语音概率
                    float speechProb = vadModel.getSpeechProbability(samples);
                    state.addProbability(speechProb);

                    // 检查是否有显著能量
                    boolean hasSignificantEnergy = hasSignificantEnergy(currentEnergy, state.getAverageEnergy());

                    // 检测语音状态变化
                    if (speechProb >= speechThreshold && hasSignificantEnergy) {
                        state.incrementConsecutiveSpeechFrames();

                        // 检查是否满足语音开始条件
                        if (state.shouldStartSpeech()) {
                            if (!state.isSpeaking()) {
                                state.setSpeaking(true);
                                state.transferPrebufferToMainBuffer();
                                logger.info("检测到语音开始 - SessionId: {}, 概率: {}", sessionId, speechProb);
                                return new VadResult(VadStatus.SPEECH_START, pcmData);
                            }
                        }

                        if (state.isSpeaking()) {
                            state.resetSilenceCount();
                            return new VadResult(VadStatus.SPEECH_CONTINUE, pcmData);
                        }
                    } else {
                        state.resetConsecutiveSpeechFrames();

                        // 如果当前是说话状态，检测静音
                        if (state.isSpeaking() && speechProb < silenceThreshold) {
                            state.incrementSilenceFrames();

                            // 计算静音持续时间
                            int silenceDurationMs = state.getSilenceDurationMs();

                            // 如果静音时间超过阈值，认为语音结束
                            if (silenceDurationMs >= minSilenceDurationMs) {
                                state.setSpeaking(false);
                                state.reset();
                                logger.info("检测到语音结束 - SessionId: {}, 静音持续: {}ms", sessionId, silenceDurationMs);
                                return new VadResult(VadStatus.SPEECH_END, pcmData);
                            }

                            // 静音但未达到结束阈值
                            return new VadResult(VadStatus.SPEECH_CONTINUE, pcmData);
                        }
                    }

                    // 检查是否超过最大静音时间（防止无限等待）
                    if (state.isSpeaking() && state.getTimeSinceLastSpeech() > maxSilenceDurationMs) {
                        state.setSpeaking(false);
                        state.reset();
                        logger.info("语音超时结束 - SessionId: {}, 超时: {}ms", sessionId, maxSilenceDurationMs);
                        return new VadResult(VadStatus.SPEECH_END, pcmData);
                    }
                }

                // 如果没有检测到明确的语音活动
                return new VadResult(
                        state.isSpeaking() ? VadStatus.SPEECH_CONTINUE : VadStatus.NO_SPEECH,
                        pcmData);
            } catch (Exception e) {
                logger.error("VAD处理音频失败 - SessionId: {}", sessionId, e);
                return new VadResult(VadStatus.ERROR, null);
            }
        }
    }

    /**
     * 应用噪声抑制
     */
    private byte[] applyNoiseReduction(String sessionId, byte[] pcmData) {
        if (tarsosNoiseReducer != null && enableNoiseReduction) {
            return tarsosNoiseReducer.processAudio(sessionId, pcmData);
        }
        return pcmData;
    }

    /**
     * 计算音频样本的能量
     */
    private float calculateEnergy(float[] samples) {
        float energy = 0;
        for (float sample : samples) {
            energy += Math.abs(sample);
        }
        return energy / samples.length;
    }

    /**
     * 判断当前能量是否显著
     */
    private boolean hasSignificantEnergy(float currentEnergy, float averageEnergy) {
        return currentEnergy > averageEnergy * 1.5 && currentEnergy > energyThreshold;
    }

    /**
     * 重置会话状态
     */
    public void resetSession(String sessionId) {
        Object lock = getSessionLock(sessionId);

        synchronized (lock) {
            VadSessionState state = sessionStates.get(sessionId);
            if (state != null) {
                state.reset();
            }
            sessionStates.remove(sessionId);

            if (enableNoiseReduction && tarsosNoiseReducer != null) {
                tarsosNoiseReducer.cleanupSession(sessionId);
            }

            sessionLocks.remove(sessionId);
            logger.info("VAD会话重置 - SessionId: {}", sessionId);
        }
    }

    /**
     * 检查当前是否正在说话
     */
    public boolean isSpeaking(String sessionId) {
        Object lock = getSessionLock(sessionId);

        synchronized (lock) {
            VadSessionState state = sessionStates.get(sessionId);
            return state != null && state.isSpeaking();
        }
    }

    /**
     * 获取当前语音概率
     */
    public float getCurrentSpeechProbability(String sessionId) {
        Object lock = getSessionLock(sessionId);

        synchronized (lock) {
            VadSessionState state = sessionStates.get(sessionId);
            if (state != null && !state.getProbabilities().isEmpty()) {
                return state.getLastProbability();
            }
            return 0.0f;
        }
    }

    // Getter和Setter方法
    public void setSpeechThreshold(float threshold) {
        if (threshold < 0.0f || threshold > 1.0f) {
            throw new IllegalArgumentException("语音阈值必须在0.0到1.0之间");
        }
        this.speechThreshold = threshold;
        this.silenceThreshold = threshold - 0.15f;
        logger.info("VAD语音阈值已更新为: {}, 静音阈值: {}", threshold, silenceThreshold);
    }

    public void setEnergyThreshold(float threshold) {
        if (threshold < 0.0f || threshold > 1.0f) {
            throw new IllegalArgumentException("能量阈值必须在0.0到1.0之间");
        }
        this.energyThreshold = threshold;
        logger.info("能量阈值已更新为: {}", threshold);
    }

    public void setEnableNoiseReduction(boolean enable) {
        this.enableNoiseReduction = enable;
        logger.info("噪声抑制功能已{}", enable ? "启用" : "禁用");
    }

    /**
     * VAD处理结果状态枚举
     */
    public enum VadStatus {
        NO_SPEECH, // 没有检测到语音
        SPEECH_START, // 检测到语音开始
        SPEECH_CONTINUE, // 语音继续中
        SPEECH_END, // 检测到语音结束
        ERROR // 处理错误
    }

    /**
     * VAD处理结果类
     */
    public static class VadResult {
        private final VadStatus status;
        private final byte[] processedData;

        public VadResult(VadStatus status, byte[] processedData) {
            this.status = status;
            this.processedData = processedData;
        }

        public VadStatus getStatus() {
            return status;
        }

        public byte[] getProcessedData() {
            return processedData;
        }

        public boolean isSpeechActive() {
            return status == VadStatus.SPEECH_START || status == VadStatus.SPEECH_CONTINUE;
        }

        public boolean isSpeechEnd() {
            return status == VadStatus.SPEECH_END;
        }
    }
}