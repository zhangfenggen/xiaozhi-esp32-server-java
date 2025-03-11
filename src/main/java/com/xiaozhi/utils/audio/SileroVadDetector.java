package com.xiaozhi.utils.audio;

import ai.onnxruntime.OrtException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

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

    // 存储每个会话的状态
    private final ConcurrentHashMap<String, SessionState> sessionStates = new ConcurrentHashMap<>();

    // 会话状态类
    private static class SessionState {
        List<Byte> audioBuffer = new ArrayList<>(); // 存储原始音频数据
        List<Float> probabilities = new ArrayList<>(); // 存储语音概率
        boolean isSpeaking = false; // 当前是否检测到语音
        long lastSpeechTimestamp = 0; // 最后一次检测到语音的时间戳
        int silenceFrameCount = 0; // 连续静音帧计数
        int frameCount = 0; // 总帧计数，用于控制日志输出频率

        // 重置状态
        public void reset() {
            audioBuffer.clear();
            probabilities.clear();
            isSpeaking = false;
            lastSpeechTimestamp = 0;
            silenceFrameCount = 0;
            frameCount = 0;
        }
    }

    public SileroVadDetector() throws OrtException {
        // 默认参数初始化
        this("src/main/resources/silero_vad.onnx", 0.5f, 16000, 500, 2000);
    }

    public SileroVadDetector(String onnxModelPath, float threshold, int samplingRate,
            int minSilenceDurationMs, int maxSilenceDurationMs) throws OrtException {
        this.model = new SileroVadOnnxModel(onnxModelPath);
        this.threshold = threshold;
        this.negThreshold = threshold - 0.15f;
        this.samplingRate = samplingRate;
        this.minSilenceDurationMs = minSilenceDurationMs;
        this.maxSilenceDurationMs = maxSilenceDurationMs;
        this.windowSizeSample = 512;
    }

    @Override
    public void initializeSession(String sessionId) {
        // 创建新的会话状态
        sessionStates.computeIfAbsent(sessionId, k -> new SessionState());
        logger.info("VAD会话初始化 - SessionId: {}", sessionId);
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
        // 获取或创建会话状态
        SessionState state = sessionStates.computeIfAbsent(sessionId, k -> new SessionState());

        // 将新的音频数据添加到缓冲区
        for (byte b : audioData) {
            state.audioBuffer.add(b);
        }

        // 如果缓冲区数据足够处理一个窗口
        if (state.audioBuffer.size() >= windowSizeSample * 2) { // 每个样本2字节(16位)
            state.frameCount++;

            // 提取一个窗口的数据
            float[] samples = new float[windowSizeSample];
            for (int i = 0; i < windowSizeSample; i++) {
                // 将两个字节转换为一个short，然后归一化为[-1,1]范围的float
                int idx = i * 2;
                short sample = (short) ((state.audioBuffer.get(idx) & 0xFF) |
                        ((state.audioBuffer.get(idx + 1) & 0xFF) << 8));
                samples[i] = sample / 32767.0f;
            }

            // 移除已处理的数据
            for (int i = 0; i < windowSizeSample * 2; i++) {
                state.audioBuffer.remove(0);
            }

            try {
                // 使用VAD模型检测语音概率
                float speechProb = model.call(new float[][] { samples }, samplingRate)[0];
                state.probabilities.add(speechProb);

                // 始终输出语音概率（每5帧输出一次，避免日志过多）
                if (state.frameCount % 5 == 0) {
                    logger.info("语音概率 - SessionId: {}, 概率: {}, 阈值: {}",
                            sessionId, speechProb, threshold);
                }

                // 检测语音状态变化
                if (speechProb >= threshold) {
                    // 如果之前不是说话状态，现在检测到说话，记录语音开始
                    if (!state.isSpeaking) {
                        logger.info("检测到语音开始 - SessionId: {}, 语音概率: {}", sessionId, speechProb);
                    }
                    state.isSpeaking = true;
                    state.lastSpeechTimestamp = System.currentTimeMillis();
                    state.silenceFrameCount = 0;
                } else if (speechProb < negThreshold && state.isSpeaking) {
                    state.silenceFrameCount++;

                    // 计算静音持续时间
                    int silenceFrames = state.silenceFrameCount;
                    int frameDurationMs = windowSizeSample * 1000 / samplingRate;
                    int silenceDurationMs = silenceFrames * frameDurationMs;

                    // 如果静音时间超过阈值，认为语音结束
                    if (silenceDurationMs >= minSilenceDurationMs) {
                        logger.info("检测到语音结束 - SessionId: {}, 静音持续: {}ms", sessionId, silenceDurationMs);

                        // 将缓冲区的数据转换为字节数组
                        byte[] completeAudio = new byte[state.audioBuffer.size()];
                        for (int i = 0; i < state.audioBuffer.size(); i++) {
                            completeAudio[i] = state.audioBuffer.get(i);
                        }

                        // 重置状态
                        state.reset();

                        // 返回完整的音频数据
                        return completeAudio;
                    }
                }

                // 检查是否超过最大静音时间（防止无限等待）
                if (state.isSpeaking &&
                        (System.currentTimeMillis() - state.lastSpeechTimestamp) > maxSilenceDurationMs) {
                    logger.info("超过最大静音时间 - SessionId: {}", sessionId);

                    // 将缓冲区的数据转换为字节数组
                    byte[] completeAudio = new byte[state.audioBuffer.size()];
                    for (int i = 0; i < state.audioBuffer.size(); i++) {
                        completeAudio[i] = state.audioBuffer.get(i);
                    }

                    // 重置状态
                    state.reset();

                    // 返回完整的音频数据
                    return completeAudio;
                }

            } catch (OrtException e) {
                logger.error("VAD处理失败", e);
            }
        }

        // 如果没有检测到语音结束，返回null
        return null;
    }

    @Override
    public void resetSession(String sessionId) {
        SessionState state = sessionStates.get(sessionId);
        if (state != null) {
            state.reset();
        }
        sessionStates.remove(sessionId);
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