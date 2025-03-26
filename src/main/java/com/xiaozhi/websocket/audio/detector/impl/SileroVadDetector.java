package com.xiaozhi.websocket.audio.detector.impl;

import ai.onnxruntime.OrtException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import com.xiaozhi.websocket.audio.detector.VadDetector;
import com.xiaozhi.websocket.audio.detector.model.SileroVadOnnxModel;
import com.xiaozhi.websocket.audio.processor.TarsosNoiseReducer;

@Component
public class SileroVadDetector implements VadDetector {
    private static final Logger logger = LoggerFactory.getLogger(SileroVadDetector.class);

    private final SileroVadOnnxModel model;
    private float threshold;
    private final float negThreshold;
    private final int samplingRate;
    private final int windowSizeSample;
    private final int minSilenceDurationMs;
    private final int maxSilenceDurationMs;

    // TarsosDSP噪声抑制处理器
    @Autowired(required = false)
    private TarsosNoiseReducer tarsosNoiseReducer;

    // 是否启用噪声抑制
    private boolean enableNoiseReduction = true;

    // 能量阈值，用于额外的能量检测
    private float energyThreshold = 0.05f;

    // 存储每个会话的状态
    private final ConcurrentHashMap<String, SessionState> sessionStates = new ConcurrentHashMap<>();

    // 会话状态类
    private static class SessionState {
        List<Byte> audioBuffer = new ArrayList<>(); // 存储原始音频数据
        List<Float> probabilities = new ArrayList<>(); // 存储语音概率
        List<Byte> preBuffer = new ArrayList<>(); // 预缓冲区
        boolean isSpeaking = false; // 当前是否检测到语音
        long lastSpeechTimestamp = 0; // 最后一次检测到语音的时间戳
        int silenceFrameCount = 0; // 连续静音帧计数
        int frameCount = 0; // 总帧计数，用于控制日志输出频率
        float averageEnergy = 0; // 平均能量
        int consecutiveSpeechFrames = 0; // 连续检测到语音的帧数
        final int requiredConsecutiveFrames = 3; // 需要连续检测到的帧数才确认为语音开始
        final int maxPreBufferSize = 32000; // 预缓冲区大小 (1秒@16kHz,16位双字节)

        // 重置状态
        public void reset() {
            audioBuffer.clear();
            probabilities.clear();
            isSpeaking = false;
            lastSpeechTimestamp = 0;
            silenceFrameCount = 0;
            frameCount = 0;
            averageEnergy = 0;
            consecutiveSpeechFrames = 0;
            preBuffer.clear();
        }
    }

    public SileroVadDetector() throws OrtException {
        // 默认参数初始化
        this("models/silero_vad.onnx", 0.6f, 16000, 500, 1000);
    }

    public SileroVadDetector(String onnxModelPath, float threshold, int samplingRate,
            int minSilenceDurationMs, int maxSilenceDurationMs) throws OrtException {
        this.model = new SileroVadOnnxModel(onnxModelPath);
        this.threshold = 0.6f; // 提高默认阈值到0.6
        this.negThreshold = this.threshold - 0.15f;
        this.samplingRate = samplingRate;
        this.minSilenceDurationMs = minSilenceDurationMs;
        this.maxSilenceDurationMs = maxSilenceDurationMs;
        this.windowSizeSample = 512;
        this.energyThreshold = 0.05f; // 提高能量阈值到0.05

        // 初始化噪声抑制器参数
        initializeNoiseReducer();
    }

    /**
     * 初始化噪声抑制器的最佳参数
     */
    private void initializeNoiseReducer() {
        if (tarsosNoiseReducer != null) {
            // 根据VAD的需求调整噪声抑制参数
            tarsosNoiseReducer.setSpectralSubtractionFactor(1.5); // 设置适中的频谱减法因子
            tarsosNoiseReducer.setNoiseEstimationFrames(10); // 设置噪声估计窗口大小
            logger.info("TarsosDSP噪声抑制器参数已优化配置");
        }
    }

    /**
     * 设置是否启用噪声抑制
     * 
     * @param enable true启用，false禁用
     */
    public void setEnableNoiseReduction(boolean enable) {
        this.enableNoiseReduction = enable;
        logger.info("噪声抑制功能已{}", enable ? "启用" : "禁用");
    }

    /**
     * 设置能量阈值
     * 
     * @param threshold 新的阈值 (0.0-1.0)
     */
    public void setEnergyThreshold(float threshold) {
        if (threshold < 0.0f || threshold > 1.0f) {
            throw new IllegalArgumentException("能量阈值必须在0.0到1.0之间");
        }
        this.energyThreshold = threshold;
        logger.info("能量阈值已更新为: {}", threshold);
    }

    @Override
    public void initializeSession(String sessionId) {
        // 创建新的会话状态
        sessionStates.computeIfAbsent(sessionId, k -> new SessionState());

        // 如果启用了降噪，初始化其会话
        if (enableNoiseReduction && tarsosNoiseReducer != null) {
            tarsosNoiseReducer.initializeSession(sessionId);
        }
    }

    @Override
    public void setThreshold(float threshold) {
        if (threshold < 0.0f || threshold > 1.0f) {
            throw new IllegalArgumentException("阈值必须在0.0到1.0之间");
        }
        this.threshold = threshold;
        logger.info("VAD阈值已更新为: {}", threshold);
    }

    @Override
    public byte[] processAudio(String sessionId, byte[] audioData) {
        // 应用噪声抑制
        byte[] processedAudio = audioData;

        // 应用TarsosDSP降噪（如果启用）
        if (enableNoiseReduction && tarsosNoiseReducer != null) {
            processedAudio = tarsosNoiseReducer.processAudio(sessionId, processedAudio);
        }

        // 获取或创建会话状态
        SessionState state = sessionStates.computeIfAbsent(sessionId, k -> new SessionState());

        // 将新的音频数据添加到预缓冲区
        for (byte b : processedAudio) {
            state.preBuffer.add(b);
            // 限制预缓冲区大小
            if (state.preBuffer.size() > state.maxPreBufferSize) {
                state.preBuffer.remove(0);
            }
        }

        // 如果已经在说话状态，则将数据添加到主缓冲区
        if (state.isSpeaking) {
            for (byte b : processedAudio) {
                state.audioBuffer.add(b);
            }
        }

        // 如果缓冲区数据足够处理一个窗口
        if (state.preBuffer.size() >= windowSizeSample * 2) { // 每个样本2字节(16位)
            state.frameCount++;

            // 提取一个窗口的数据用于分析
            float[] samples = new float[windowSizeSample];
            float currentEnergy = 0;

            // 从预缓冲区中提取最新的一个窗口数据
            int startIdx = state.preBuffer.size() - windowSizeSample * 2;
            for (int i = 0; i < windowSizeSample; i++) {
                // 将两个字节转换为一个short，然后归一化为[-1,1]范围的float
                int idx = startIdx + i * 2;
                short sample = (short) ((state.preBuffer.get(idx) & 0xFF) |
                        ((state.preBuffer.get(idx + 1) & 0xFF) << 8));
                samples[i] = sample / 32767.0f;

                // 计算能量
                currentEnergy += Math.abs(samples[i]);
            }

            // 平均能量
            currentEnergy /= windowSizeSample;

            // 更新平均能量（使用指数移动平均）
            if (state.averageEnergy == 0) {
                state.averageEnergy = currentEnergy;
            } else {
                state.averageEnergy = 0.95f * state.averageEnergy + 0.05f * currentEnergy;
            }

            try {
                // 使用VAD模型检测语音概率
                float speechProb = model.call(new float[][] { samples }, samplingRate)[0];
                state.probabilities.add(speechProb);

                // 检查能量是否足够（相对于平均能量）
                boolean hasSignificantEnergy = currentEnergy > state.averageEnergy * 1.5
                        && currentEnergy > energyThreshold;

                // 始终输出语音概率（每5帧输出一次，避免日志过多）
                // if (state.frameCount % 5 == 0) {
                // logger.info("语音概率 - SessionId: {}, 概率: {}, 阈值: {}, 能量: {}, 平均能量: {}, 能量显著:
                // {}",
                // sessionId, speechProb, threshold, currentEnergy, state.averageEnergy,
                // hasSignificantEnergy);
                // }

                // 检测语音状态变化 - 同时考虑VAD和能量
                if (speechProb >= threshold && hasSignificantEnergy) {
                    state.consecutiveSpeechFrames++;

                    // 只有连续多帧都检测到语音，才认为语音真正开始
                    if (state.consecutiveSpeechFrames >= state.requiredConsecutiveFrames && !state.isSpeaking) {

                        state.isSpeaking = true;

                        // 将预缓冲区的数据复制到主缓冲区
                        state.audioBuffer.addAll(state.preBuffer);
                    }

                    if (state.isSpeaking) {
                        state.lastSpeechTimestamp = System.currentTimeMillis();
                        state.silenceFrameCount = 0;
                    }
                } else {
                    // 重置连续语音帧计数
                    state.consecutiveSpeechFrames = 0;

                    // 如果当前是说话状态，检测静音
                    if (state.isSpeaking && speechProb < negThreshold) {
                        state.silenceFrameCount++;

                        // 计算静音持续时间
                        int silenceFrames = state.silenceFrameCount;
                        int frameDurationMs = windowSizeSample * 1000 / samplingRate;
                        int silenceDurationMs = silenceFrames * frameDurationMs;

                        // 如果静音时间超过阈值，认为语音结束
                        if (silenceDurationMs >= minSilenceDurationMs) {

                            // 将缓冲区的数据转换为字节数组
                            byte[] completeAudio = new byte[state.audioBuffer.size()];
                            for (int i = 0; i < state.audioBuffer.size(); i++) {
                                completeAudio[i] = state.audioBuffer.get(i);
                            }

                            // 对音频进行后处理
                            byte[] processedResult = postProcessAudio(completeAudio);

                            // 重置状态
                            state.reset();

                            // 返回处理后的音频数据
                            return processedResult;
                        }
                    }
                }

                // 检查是否超过最大静音时间（防止无限等待）
                if (state.isSpeaking &&
                        (System.currentTimeMillis() - state.lastSpeechTimestamp) > maxSilenceDurationMs) {

                    // 将缓冲区的数据转换为字节数组
                    byte[] completeAudio = new byte[state.audioBuffer.size()];
                    for (int i = 0; i < state.audioBuffer.size(); i++) {
                        completeAudio[i] = state.audioBuffer.get(i);
                    }

                    // 对音频进行后处理
                    byte[] processedResult = postProcessAudio(completeAudio);

                    // 重置状态
                    state.reset();

                    // 返回处理后的音频数据
                    return processedResult;
                }

            } catch (OrtException e) {
                logger.error("VAD处理失败", e);
            }
        }

        // 如果没有检测到语音结束，返回null
        return null;
    }

    /**
     * 对录制的音频进行后处理，去除低能量部分
     * 
     * @param audioData 原始音频数据
     * @return 处理后的音频数据
     */
    private byte[] postProcessAudio(byte[] audioData) {
        if (audioData == null || audioData.length < 4) {
            return audioData;
        }

        // 将字节数据转换为short数组
        short[] samples = new short[audioData.length / 2];
        for (int i = 0; i < samples.length; i++) {
            samples[i] = (short) ((audioData[i * 2] & 0xFF) | ((audioData[i * 2 + 1] & 0xFF) << 8));
        }

        // 计算平均能量
        float avgEnergy = 0;
        for (short sample : samples) {
            avgEnergy += Math.abs(sample) / 32767.0f;
        }
        avgEnergy /= samples.length;

        // 找到高能量段的起始和结束位置
        int start = 0, end = samples.length - 1;
        float threshold = avgEnergy * 2.0f; // 能量阈值为平均能量的2倍

        // 从前向后找到第一个高能量点
        for (int i = 0; i < samples.length; i++) {
            if (Math.abs(samples[i]) / 32767.0f > threshold) {
                start = Math.max(0, i - 1600); // 向前预留0.1秒
                break;
            }
        }

        // 从后向前找到最后一个高能量点
        for (int i = samples.length - 1; i >= 0; i--) {
            if (Math.abs(samples[i]) / 32767.0f > threshold) {
                end = Math.min(samples.length - 1, i + 1600); // 向后预留0.1秒
                break;
            }
        }

        // 如果找不到高能量段，或者高能量段太短，返回原始数据
        if (end - start < 3200) { // 至少0.2秒
            return audioData;
        }

        // 截取高能量段
        int newLength = (end - start + 1) * 2;
        byte[] trimmedAudio = new byte[newLength];
        System.arraycopy(audioData, start * 2, trimmedAudio, 0, newLength);

        return trimmedAudio;
    }

    @Override
    public void resetSession(String sessionId) {
        SessionState state = sessionStates.get(sessionId);
        if (state != null) {
            state.reset();
        }
        sessionStates.remove(sessionId);

        // 如果启用了降噪，重置其会话
        if (enableNoiseReduction && tarsosNoiseReducer != null) {
            tarsosNoiseReducer.cleanupSession(sessionId);
        }

        logger.info("VAD会话重置 - SessionId: {}", sessionId);
    }

    @Override
    public boolean isSpeaking(String sessionId) {
        SessionState state = sessionStates.get(sessionId);
        return state != null && state.isSpeaking;
    }

    @Override
    public float getCurrentSpeechProbability(String sessionId) {
        SessionState state = sessionStates.get(sessionId);
        if (state != null && !state.probabilities.isEmpty()) {
            return state.probabilities.get(state.probabilities.size() - 1);
        }
        return 0.0f;
    }

    /**
     * 强制结束当前语音，返回已收集的音频数据
     */
    public byte[] forceEndSession(String sessionId) {
        SessionState state = sessionStates.get(sessionId);
        if (state != null && !state.audioBuffer.isEmpty()) {
            byte[] completeAudio = new byte[state.audioBuffer.size()];
            for (int i = 0; i < state.audioBuffer.size(); i++) {
                completeAudio[i] = state.audioBuffer.get(i);
            }
            state.reset();
            return completeAudio;
        }
        return null;
    }
}