package com.xiaozhi.websocket.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.xiaozhi.utils.AudioUtils;
import com.xiaozhi.utils.OpusProcessor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.socket.WebSocketMessage;
import org.springframework.web.reactive.socket.WebSocketSession;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.locks.LockSupport;

/**
 * 音频服务，负责处理音频的非流式发送
 */
@Service
public class AudioService {
    private static final Logger logger = LoggerFactory.getLogger(AudioService.class);

    // 恢复原始的帧间隔时间，与OpusProcessor中的OPUS_FRAME_DURATION_MS保持一致
    private static final long OPUS_FRAME_INTERVAL_MS = 60;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private OpusProcessor opusProcessor;

    @Autowired
    private SessionManager sessionManager;

    // 存储每个会话最后一次发送帧的时间戳
    private final Map<String, AtomicLong> lastFrameSentTime = new ConcurrentHashMap<>();

    // 存储每个会话当前是否正在播放音频
    private final Map<String, Boolean> isPlaying = new ConcurrentHashMap<>();

    /**
     * 发送TTS开始消息
     */
    public Mono<Void> sendStart(WebSocketSession session) {
        Map<String, Object> message = new HashMap<>();
        message.put("type", "tts");
        message.put("state", "start");

        try {
            String json = objectMapper.writeValueAsString(message);
            return session.send(Mono.just(session.textMessage(json)));
        } catch (Exception e) {
            logger.error("发送TTS开始消息失败", e);
            return Mono.empty();
        }
    }

    /**
     * 发送TTS句子开始消息
     */
    public Mono<Void> sendSentenceStart(WebSocketSession session, String text) {
        Map<String, Object> message = new HashMap<>();
        message.put("type", "tts");
        message.put("state", "sentence_start");
        message.put("text", text);

        try {
            String json = objectMapper.writeValueAsString(message);
            return session.send(Mono.just(session.textMessage(json)));
        } catch (Exception e) {
            logger.error("发送TTS句子开始消息失败", e);
            return Mono.empty();
        }
    }

    /**
     * 发送停止消息
     */
    public Mono<Void> sendStop(WebSocketSession session) {
        String sessionId = session.getId();
        Map<String, Object> message = new HashMap<>();
        message.put("type", "tts");
        message.put("state", "stop");

        try {
            String json = objectMapper.writeValueAsString(message);
            // 标记播放结束
            isPlaying.put(sessionId, false);
            return session.send(Mono.just(session.textMessage(json)));
        } catch (Exception e) {
            logger.error("发送停止消息失败", e);
            isPlaying.put(sessionId, false);
            return Mono.empty();
        }
    }

    /**
     * 检查会话是否正在播放音频
     */
    public boolean isPlaying(String sessionId) {
        return Boolean.TRUE.equals(isPlaying.getOrDefault(sessionId, false));
    }

    /**
     * 发送音频消息
     * 
     * @param session   WebSocketSession会话
     * @param audioPath 音频文件路径
     * @param text      对应的文本
     * @param isFirst   是否是开始消息
     * @param isLast    是否是结束消息
     * @return 操作完成的Mono
     */
    public Mono<Void> sendAudioMessage(
            WebSocketSession session,
            String audioPath,
            String text,
            boolean isFirst,
            boolean isLast) {

        String sessionId = session.getId();

        // 标记开始播放
        isPlaying.put(sessionId, true);

        if (isFirst) {
            sendStart(session);
        }

        if (audioPath == null) {
            // 如果没有音频路径但是结束消息，发送结束标记
            if (isLast) {
                return sendStop(session);
            }
            isPlaying.put(sessionId, false);
            return Mono.empty();
        }
        sessionManager.updateLastActivity(sessionId); // 更新活动时间
        return sendSentenceStart(session, text)
                .then(Mono.fromCallable(() -> {
                    String fullPath = audioPath;
                    File audioFile = new File(fullPath);

                    if (!audioFile.exists()) {
                        logger.warn("音频文件不存在: {}", fullPath);
                        return null;
                    }

                    List<byte[]> opusFrames;

                    if (audioPath.contains(".opus")) {
                        // 如果是opus文件，直接读取opus帧数据
                        try {
                            opusFrames = opusProcessor.readOpus(audioFile);
                        } catch (IOException e) {
                            logger.error("读取Opus文件失败: {}", fullPath, e);
                            return null;
                        }
                    } else {
                        // 如果不是opus文件，按照原来的逻辑处理
                        byte[] audioData = AudioUtils.readAsPcm(fullPath);
                        // 将PCM转换为Opus帧
                        opusFrames = opusProcessor.pcmToOpus(
                                session.getId(), audioData);
                    }

                    return opusFrames;
                })
                        .subscribeOn(Schedulers.boundedElastic())
                        .flatMap(opusFrames -> {
                            if (opusFrames == null || opusFrames.isEmpty()) {
                                if (isLast) {
                                    return sendStop(session);
                                }
                                isPlaying.put(sessionId, false);
                                return Mono.empty();
                            }

                            // 重置发送时间以确保第一帧立即发送
                            lastFrameSentTime.computeIfAbsent(sessionId, k -> new AtomicLong(0)).set(0);

                            // 发送所有Opus帧
                            for (int i = 0; i < opusFrames.size(); i++) {
                                sendOpusFrame(session, opusFrames.get(i));
                            }
                            isPlaying.put(sessionId, false);

                            // 如果是结束消息，发送TTS结束消息
                            if (isLast) {
                                return sendStop(session);
                            }

                            return Mono.empty();
                        })
                        .doFinally(signalType -> {
                            if (sessionManager.isCloseAfterChat(sessionId)) {
                                sessionManager.closeSession(sessionId);
                            }
                        })
                        .onErrorResume(error -> {
                            logger.error("处理音频消息时发生错误 - SessionId: {}", sessionId, error);
                            // 如果发生错误但仍然是结束消息，确保发送stop
                            if (isLast) {
                                return sendStop(session);
                            }
                            isPlaying.put(sessionId, false);
                            return Mono.empty();
                        }));
    }

    /**
     * 发送Opus帧数据，包含速率控制
     */
    private void sendOpusFrame(WebSocketSession session, byte[] opusFrame) {
        try {
            String sessionId = session.getId();
            AtomicLong lastSent = lastFrameSentTime.computeIfAbsent(
                    sessionId, k -> new AtomicLong(0));

            // 计算需要等待的时间
            long currentTime = System.currentTimeMillis();
            long lastTime = lastSent.get();
            long waitTime = 0;

            if (lastTime > 0) {
                waitTime = OPUS_FRAME_INTERVAL_MS - (currentTime - lastTime);
                if (waitTime > 0) {
                    if (waitTime <= 0) {
                        return;
                    }

                    long nanos = TimeUnit.MILLISECONDS.toNanos(waitTime);
                    final long start = System.nanoTime();
                    final long end = start + nanos;
                    long sleepTime;

                    // 使用分段睡眠策略，提高精度
                    while (true) {
                        sleepTime = end - System.nanoTime();
                        if (sleepTime <= 0) {
                            break;
                        }

                        if (sleepTime > 10_000_000) { // 大于10毫秒，使用Thread.sleep
                            try {
                                Thread.sleep(Math.max(1, sleepTime / 1_000_000 - 1));
                            } catch (InterruptedException e) {
                                Thread.currentThread().interrupt();
                                return;
                            }
                        } else if (sleepTime > 1_000_000) { // 大于1毫秒，使用LockSupport.parkNanos
                            LockSupport.parkNanos(sleepTime - 500_000); // 留出500微秒的余量
                        } else { // 小于1毫秒，使用自旋
                            // 自旋等待剩余时间
                            long spinStart = System.nanoTime();
                            while (System.nanoTime() - spinStart < sleepTime) {
                                // 空循环，让CPU自旋
                            }
                            break;
                        }
                    }
                }
            }

            // 直接发送原始Opus帧数据作为二进制消息
            WebSocketMessage wsMessage = session.binaryMessage(
                    factory -> factory.wrap(opusFrame));

            // 使用错误处理，避免因连接关闭导致的错误日志
            session.send(Mono.just(wsMessage))
                    .subscribe(
                            null,
                            error -> {
                                // 只有当不是连接关闭错误时才记录日志
                                if (!(error instanceof reactor.netty.channel.AbortedException) ||
                                        !error.getMessage()
                                                .contains("Connection has been closed BEFORE send operation")) {
                                    logger.error("发送Opus帧失败", error);
                                } else {
                                    // 标记播放已停止
                                    isPlaying.put(sessionId, false);
                                }
                            });

            // 更新最后发送时间
            lastSent.set(System.currentTimeMillis());

        } catch (Exception e) {
            logger.error("发送Opus帧失败", e);
        }
    }

    /**
     * 清理会话资源
     */
    public void cleanupSession(String sessionId) {
        lastFrameSentTime.remove(sessionId);
        isPlaying.remove(sessionId);
    }
}