package com.xiaozhi.websocket.service;

import com.xiaozhi.websocket.audio.detector.VadDetector;
import com.xiaozhi.websocket.audio.processor.OpusProcessor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.core.io.buffer.DataBufferFactory;
import org.springframework.core.io.buffer.DefaultDataBufferFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.socket.WebSocketMessage;
import org.springframework.web.reactive.socket.WebSocketSession;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicBoolean;

@Service
public class AudioService {

    private static final Logger logger = LoggerFactory.getLogger(AudioService.class);

    // 播放帧持续时间（毫秒）
    private static final int FRAME_DURATION_MS = 60;

    // 默认音频采样率和通道数
    private static final int DEFAULT_SAMPLE_RATE = 16000;
    private static final int DEFAULT_CHANNELS = 1;

    // 为每个会话维护一个音频发送队列
    private final Map<String, Queue<AudioProcessTask>> audioQueues = new ConcurrentHashMap<>();

    // 跟踪每个会话的发送状态
    private final Map<String, AtomicBoolean> isProcessingMap = new ConcurrentHashMap<>();

    // 用于存储会话的音频处理状态
    private final ConcurrentHashMap<String, AudioProcessingState> sessionStates = new ConcurrentHashMap<>();

    @Autowired
    private MessageService messageService;

    @Autowired
    private OpusProcessor opusProcessor;

    @Autowired
    private VadDetector vadDetector;

    private final DataBufferFactory bufferFactory = new DefaultDataBufferFactory();

    /**
     * 音频处理状态类
     */
    private static class AudioProcessingState {
        private long lastProcessTime = System.currentTimeMillis();

        public long getLastProcessTime() {
            return lastProcessTime;
        }

        public void updateProcessTime() {
            this.lastProcessTime = System.currentTimeMillis();
        }
    }

    /**
     * 音频处理任务类，包含已转码的音频数据
     */
    private static class AudioProcessTask {
        private final List<byte[]> opusFrames;
        private final String text;
        private final boolean isFirstText;
        private final boolean isLastText;
        private final String originalFilePath;

        public AudioProcessTask(List<byte[]> opusFrames, String text, boolean isFirstText, boolean isLastText,
                              String originalFilePath) {
            this.opusFrames = opusFrames;
            this.text = text;
            this.isFirstText = isFirstText;
            this.isLastText = isLastText;
            this.originalFilePath = originalFilePath;
        }

        public List<byte[]> getOpusFrames() {
            return opusFrames;
        }

        public String getText() {
            return text;
        }

        public boolean isFirstText() {
            return isFirstText;
        }

        public boolean isLastText() {
            return isLastText;
        }

        public String getOriginalFilePath() {
            return originalFilePath;
        }
    }

    /**
     * 音频处理结果类
     */
    public static class AudioProcessResult {
        private List<byte[]> opusFrames;
        private long durationMs;

        public AudioProcessResult() {
            this.opusFrames = new ArrayList<>();
            this.durationMs = 0;
        }

        public AudioProcessResult(List<byte[]> opusFrames, long durationMs) {
            this.opusFrames = opusFrames;
            this.durationMs = durationMs;
        }

        public List<byte[]> getOpusFrames() {
            return opusFrames;
        }

        public long getDurationMs() {
            return durationMs;
        }
    }

    /**
     * 初始化会话的音频处理
     */
    public void initializeSession(String sessionId) {
        vadDetector.initializeSession(sessionId);
        // 初始化音频队列和处理状态
        audioQueues.putIfAbsent(sessionId, new ConcurrentLinkedQueue<>());
        isProcessingMap.putIfAbsent(sessionId, new AtomicBoolean(false));
        sessionStates.putIfAbsent(sessionId, new AudioProcessingState());
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
            // 确保会话状态已初始化
            initializeSession(sessionId);
            
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
     * 清理会话的音频处理状态
     */
    public void cleanupSession(String sessionId) {
        vadDetector.resetSession(sessionId);
        opusProcessor.cleanupSession(sessionId);
        // 清理音频队列
        Queue<AudioProcessTask> queue = audioQueues.get(sessionId);
        if (queue != null) {
            queue.clear();
        }
        // 重置处理状态
        AtomicBoolean isProcessing = isProcessingMap.get(sessionId);
        if (isProcessing != null) {
            isProcessing.set(false);
        }
        // 移除会话状态
        sessionStates.remove(sessionId);
    }

    /**
     * 处理音频文件，提取PCM数据并转换为Opus格式
     * 
     * @param audioFilePath 音频文件路径
     * @param sampleRate    采样率
     * @param channels      通道数
     * @return 处理结果，包含opus数据和持续时间
     */
    public AudioProcessResult processAudioFile(String audioFilePath, int sampleRate, int channels) {
        try {
            // 从音频文件获取PCM数据
            byte[] pcmData = extractPcmFromAudio(audioFilePath);
            if (pcmData == null) {
                logger.error("无法从文件提取PCM数据: {}", audioFilePath);
                return new AudioProcessResult();
            }

            // 计算音频时长
            long durationMs = calculateDuration(pcmData, sampleRate, channels);

            // 转换为Opus格式
            List<byte[]> opusFrames = opusProcessor.convertPcmToOpus(pcmData, sampleRate, channels, FRAME_DURATION_MS);

            return new AudioProcessResult(opusFrames, durationMs);
        } catch (Exception e) {
            logger.error("处理音频文件失败: {}", audioFilePath, e);
            return new AudioProcessResult();
        }
    }

    /**
     * 从音频文件中提取PCM数据
     */
    public byte[] extractPcmFromAudio(String audioFilePath) {
        try {
            // 创建临时PCM文件
            File tempPcmFile = File.createTempFile("temp_pcm_extract_", ".pcm");
            String tempPcmPath = tempPcmFile.getAbsolutePath();
            try {
                // 使用FFmpeg直接将音频转换为PCM
                String[] command = {
                        "ffmpeg",
                        "-i", audioFilePath,
                        "-f", "s16le", // 16位有符号小端格式
                        "-acodec", "pcm_s16le",
                        "-y",
                        tempPcmPath
                };

                ProcessBuilder pb = new ProcessBuilder(command);
                pb.redirectErrorStream(true);
                Process process = pb.start();
                // 等待进程完成
                int exitCode = process.waitFor();
                if (exitCode != 0) {
                    logger.error("FFmpeg提取PCM数据失败，退出码: {}", exitCode);
                    return null;
                }

                // 读取PCM文件内容
                return Files.readAllBytes(Paths.get(tempPcmPath));
            } finally {
                // 删除临时文件
                tempPcmFile.delete();
            }
        } catch (Exception e) {
            logger.error("从音频文件提取PCM数据失败: {}", audioFilePath, e);
            return null;
        }
    }

    /**
     * 计算音频时长
     */
    public long calculateDuration(byte[] pcmData, int sampleRate, int channels) {
        // 16位采样
        int bytesPerSample = 2;
        return (long) ((pcmData.length * 1000.0) / (sampleRate * channels * bytesPerSample));
    }

    /**
     * 发送音频消息
     * 
     * @param session WebSocket会话
     * @param audioFilePath 音频文件路径
     * @param text 文本内容
     * @param isStart 是否是开始
     * @param isEnd 是否是结束
     * @return Mono<Void> 操作结果
     */
    public Mono<Void> sendAudioMessage(WebSocketSession session, String audioFilePath, String text, boolean isStart, boolean isEnd) {
        return Mono.fromCallable(() -> processAudioFile(audioFilePath, DEFAULT_SAMPLE_RATE, DEFAULT_CHANNELS))
                .subscribeOn(Schedulers.boundedElastic())
                .flatMap(audioResult -> {
                    List<WebSocketMessage> messages = new ArrayList<>();
                    
                    // 如果是开始，添加开始标记
                    if (isStart) {
                        messages.add(createTextMessage(session, "audio", "start", text));
                    }
                    
                    // 添加音频数据消息
                    for (byte[] frame : audioResult.getOpusFrames()) {
                        DataBuffer buffer = bufferFactory.wrap(frame);
                        messages.add(session.binaryMessage(factory -> buffer));
                    }
                    
                    // 如果是结束，添加结束标记
                    if (isEnd) {
                        messages.add(createTextMessage(session, "audio", "end", text));
                    }
                    
                    // 创建消息发送流，并添加时间间隔
                    return session.send(
                        Flux.fromIterable(messages)
                            .delayElements(Duration.ofMillis(FRAME_DURATION_MS))
                    ).doFinally(signalType -> {
                        // 删除音频文件
                        deleteAudioFiles(audioFilePath);
                    });
                })
                .onErrorResume(e -> {
                    logger.error("发送音频消息失败: {}", e.getMessage(), e);
                    return Mono.empty();
                });
    }

    /**
     * 创建文本消息
     */
    private WebSocketMessage createTextMessage(WebSocketSession session, String type, String state, String text) {
        StringBuilder jsonBuilder = new StringBuilder();
        jsonBuilder.append("{\"type\":\"").append(type).append("\",\"state\":\"").append(state).append("\"");
        if (text != null && !text.isEmpty()) {
            jsonBuilder.append(",\"text\":\"").append(text.replace("\"", "\\\"")).append("\"");
        }
        jsonBuilder.append("}");
        return session.textMessage(jsonBuilder.toString());
    }

    /**
     * 删除音频文件及其相关文件（如同名的VTT文件）
     * 
     * @param audioPath 音频文件路径
     * @return 是否成功删除
     */
    public boolean deleteAudioFiles(String audioPath) {
        if (audioPath == null) {
            return false;
        }
        
        try {
            boolean success = true;
            
            // 删除原始音频文件
            File audioFile = new File(audioPath);
            if (audioFile.exists()) {
                if (!audioFile.delete()) {
                    logger.warn("无法删除音频文件: {}", audioPath);
                    success = false;
                }
            }

            // 删除可能存在的VTT文件
            File vttFile = new File(audioPath + ".vtt");
            if (vttFile.exists()) {
                if (!vttFile.delete()) {
                    logger.warn("无法删除VTT文件: {}", vttFile.getPath());
                    success = false;
                }
            }
            
            return success;
        } catch (Exception e) {
            logger.error("删除音频文件时发生错误: {}", audioPath, e);
            return false;
        }
    }

    /**
     * 发送TTS句子开始指令（包含文本）
     */
    public Mono<Void> sendSentenceStart(WebSocketSession session, String text) {
        try {
            StringBuilder jsonBuilder = new StringBuilder();
            jsonBuilder.append("{\"type\":\"tts\",\"state\":\"sentence_start\"");
            if (text != null && !text.isEmpty()) {
                jsonBuilder.append(",\"text\":\"").append(text.replace("\"", "\\\"")).append("\"");
            }
            jsonBuilder.append("}");
            return session.send(Mono.just(session.textMessage(jsonBuilder.toString())));
        } catch (Exception e) {
            logger.error("发送句子开始消息失败", e);
            return Mono.empty();
        }
    }

    /**
     * 发送TTS句子结束指令
     */
    public Mono<Void> sendSentenceEnd(WebSocketSession session, String text) {
        try {
            StringBuilder jsonBuilder = new StringBuilder();
            jsonBuilder.append("{\"type\":\"tts\",\"state\":\"sentence_end\"");
            if (text != null && !text.isEmpty()) {
                jsonBuilder.append(",\"text\":\"").append(text.replace("\"", "\\\"")).append("\"");
            }
            jsonBuilder.append("}");
            return session.send(Mono.just(session.textMessage(jsonBuilder.toString())));
        } catch (Exception e) {
            logger.error("发送句子结束消息失败", e);
            return Mono.empty();
        }
    }

    /**
     * 发送TTS开始指令
     */
    public Mono<Void> sendStart(WebSocketSession session) {
        try {
            return session.send(Mono.just(session.textMessage("{\"type\":\"tts\",\"state\":\"start\"}")));
        } catch (Exception e) {
            logger.error("发送开始消息失败", e);
            return Mono.empty();
        }
    }

    /**
     * 发送TTS停止指令
     */
    public Mono<Void> sendStop(WebSocketSession session) {
        if (session == null || !session.isOpen()) {
            logger.warn("尝试发送停止指令到无效的WebSocket会话");
            return Mono.empty();
        }

        String sessionId = session.getId();

        // 清空音频队列
        Queue<AudioProcessTask> queue = audioQueues.get(sessionId);
        if (queue != null) {
            // 保存需要删除的音频文件路径
            List<String> filesToDelete = new ArrayList<>();
            queue.forEach(task -> {
                if (task != null && task.getOriginalFilePath() != null) {
                    filesToDelete.add(task.getOriginalFilePath());
                }
            });

            // 清空队列
            queue.clear();

            // 异步删除文件
            if (!filesToDelete.isEmpty()) {
                Mono.fromRunnable(() -> filesToDelete.forEach(this::deleteAudioFiles))
                    .subscribeOn(Schedulers.boundedElastic())
                    .subscribe();
            }
        }

        // 发送停止指令
        try {
            return session.send(Mono.just(session.textMessage("{\"type\":\"tts\",\"state\":\"stop\"}")));
        } catch (Exception e) {
            logger.error("发送停止消息失败", e);
            return Mono.empty();
        }
    }

    /**
     * 检查当前是否正在说话
     */
    public boolean isSpeaking(String sessionId) {
        return vadDetector.isSpeaking(sessionId);
    }

    /**
     * 获取当前语音概率
     */
    public float getCurrentSpeechProbability(String sessionId) {
        return vadDetector.getCurrentSpeechProbability(sessionId);
    }
}