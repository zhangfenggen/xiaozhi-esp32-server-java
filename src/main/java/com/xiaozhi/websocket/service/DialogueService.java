package com.xiaozhi.websocket.service;

import com.xiaozhi.entity.SysConfig;
import com.xiaozhi.entity.SysDevice;
import com.xiaozhi.websocket.llm.LlmManager;
import com.xiaozhi.websocket.service.VadService.VadStatus;
import com.xiaozhi.websocket.stt.SttService;
import com.xiaozhi.websocket.stt.factory.SttServiceFactory;
import com.xiaozhi.websocket.tts.factory.TtsServiceFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.reactive.socket.WebSocketSession;
import reactor.core.publisher.Mono;
import reactor.core.publisher.Sinks;
import reactor.core.scheduler.Schedulers;

import java.util.Map;
import java.util.Queue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.text.DecimalFormat;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentSkipListMap;

/**
 * 对话处理服务
 * 负责处理语音识别和对话生成的业务逻辑
 */
@Service
public class DialogueService {
    private static final Logger logger = LoggerFactory.getLogger(DialogueService.class);

    // 用于格式化浮点数，保留2位小数
    private static final DecimalFormat df = new DecimalFormat("0.00");

    @Autowired
    private LlmManager llmManager;

    @Autowired
    private AudioService audioService;

    @Autowired
    private TtsServiceFactory ttsService;

    @Autowired
    private SttServiceFactory sttServiceFactory;

    @Autowired
    private MessageService messageService;

    @Autowired
    private VadService vadService;

    @Autowired
    private SessionManager sessionManager;

    // 添加一个每个会话的句子序列号计数器
    private final Map<String, AtomicInteger> sessionSentenceCounters = new ConcurrentHashMap<>();

    // 添加会话的语音识别开始时间记录
    private final Map<String, Long> sessionSttStartTimes = new ConcurrentHashMap<>();

    // 添加会话的模型回复开始时间记录
    private final Map<String, Long> sessionLlmStartTimes = new ConcurrentHashMap<>();

    // 添加会话的TTS开始时间记录
    private final Map<String, Map<Integer, Long>> sessionTtsStartTimes = new ConcurrentHashMap<>();

    // 添加会话的完整回复内容
    private final Map<String, StringBuilder> sessionFullResponses = new ConcurrentHashMap<>();

    // 添加会话的句子处理队列，确保按顺序处理
    private final Map<String, Queue<SentenceTask>> sessionSentenceQueues = new ConcurrentHashMap<>();

    // 添加会话的处理状态标志
    private final Map<String, AtomicBoolean> sessionProcessingFlags = new ConcurrentHashMap<>();

    // 添加会话的音频准备映射，用于存储已准备好的音频路径
    private final Map<String, ConcurrentSkipListMap<Integer, String>> sessionPreparedAudio = new ConcurrentHashMap<>();

    // 添加会话的音频准备状态映射
    private final Map<String, Map<Integer, CompletableFuture<String>>> sessionAudioFutures = new ConcurrentHashMap<>();

    /**
     * 句子任务类，用于按顺序处理句子
     */
    private static class SentenceTask {
        private final String sentence;
        private final boolean isStart;
        private final boolean isEnd;
        private final SysConfig ttsConfig;
        private final String voiceName;
        private final int sentenceNumber;

        public SentenceTask(String sentence, boolean isStart, boolean isEnd,
                SysConfig ttsConfig, String voiceName, int sentenceNumber) {
            this.sentence = sentence;
            this.isStart = isStart;
            this.isEnd = isEnd;
            this.ttsConfig = ttsConfig;
            this.voiceName = voiceName;
            this.sentenceNumber = sentenceNumber;
        }
    }

    /**
     * 处理音频数据
     * 
     * @param session  WebSocket会话
     * @param opusData Opus格式的音频数据
     * @return 处理结果
     */
    public Mono<Void> processAudioData(WebSocketSession session, byte[] opusData) {
        String sessionId = session.getId();
        SysDevice device = sessionManager.getDeviceConfig(sessionId);

        // 如果设备未注册或不在监听状态，忽略音频数据
        if (device == null || !sessionManager.isListening(sessionId)) {
            return Mono.empty();
        }

        SysConfig sttConfig = null;
        SysConfig ttsConfig = null;

        if (device.getSttId() != null) {
            sttConfig = sessionManager.getCachedConfig(device.getSttId());
        }

        if (device.getTtsId() != null) {
            ttsConfig = sessionManager.getCachedConfig(device.getTtsId());
        }

        // 创建最终变量以在lambda中使用
        final SysConfig finalSttConfig = sttConfig;
        final SysConfig finalTtsConfig = ttsConfig;

        return Mono.fromCallable(() -> {
            // 使用VAD处理音频数据
            return vadService.processAudio(sessionId, opusData);
        })
                .subscribeOn(Schedulers.boundedElastic())
                .flatMap(vadResult -> {
                    // 如果VAD处理出错，直接返回
                    if (vadResult.getStatus() == VadStatus.ERROR || vadResult.getProcessedData() == null) {
                        return Mono.empty();
                    }
                    // 检测到语音
                    sessionManager.updateLastActivity(sessionId);
                    // 根据VAD状态处理
                    switch (vadResult.getStatus()) {
                        case SPEECH_START:
                            // 检测到语音开始，记录开始时间
                            sessionSttStartTimes.put(sessionId, System.currentTimeMillis());
                            logger.info("语音识别开始 - SessionId: {}", sessionId);

                            // 初始化流式识别
                            return initializeStreamingRecognition(session, sessionId, finalSttConfig, finalTtsConfig,
                                    device,
                                    vadResult.getProcessedData());

                        case SPEECH_CONTINUE:
                            // 语音继续，发送数据到流式识别
                            if (sessionManager.isStreaming(sessionId)) {
                                Sinks.Many<byte[]> audioSink = sessionManager.getAudioSink(sessionId);
                                if (audioSink != null) {
                                    audioSink.tryEmitNext(vadResult.getProcessedData());
                                }
                            }
                            return Mono.empty();

                        case SPEECH_END:
                            // 语音结束，完成流式识别
                            if (sessionManager.isStreaming(sessionId)) {
                                Sinks.Many<byte[]> audioSink = sessionManager.getAudioSink(sessionId);
                                if (audioSink != null) {
                                    audioSink.tryEmitComplete();
                                    sessionManager.setStreamingState(sessionId, false);
                                }
                            }
                            return Mono.empty();

                        default:
                            return Mono.empty();
                    }
                })
                .onErrorResume(e -> {
                    logger.error("处理音频数据失败: {}", e.getMessage(), e);
                    return Mono.empty();
                });
    }

    /**
     * 初始化流式语音识别
     */
    private Mono<Void> initializeStreamingRecognition(
            WebSocketSession session,
            String sessionId,
            SysConfig sttConfig,
            SysConfig ttsConfig,
            SysDevice device,
            byte[] initialAudio) {

        // 如果已经在进行流式识别，先清理旧的资源
        sessionManager.closeAudioSink(sessionId);

        // 创建新的音频数据接收器
        Sinks.Many<byte[]> audioSink = sessionManager.createAudioSink(sessionId);
        sessionManager.setStreamingState(sessionId, true);

        // 获取对应的STT服务
        SttService sttService = sttServiceFactory.getSttService(sttConfig);

        if (sttService == null) {
            logger.error("无法获取STT服务 - Provider: {}", sttConfig != null ? sttConfig.getProvider() : "null");
            return Mono.empty();
        }

        // 发送初始音频数据
        if (initialAudio != null && initialAudio.length > 0) {
            audioSink.tryEmitNext(initialAudio);
        }

        // 创建最终变量以在lambda中使用
        final SysConfig finalTtsConfig = ttsConfig;

        // 启动流式识别
        sttService.streamRecognition(audioSink.asFlux())
                .doOnNext(text -> {
                    // 发送中间识别结果
                    if (StringUtils.hasText(text)) {
                        messageService.sendMessage(session, "stt", "interim", text).subscribe();
                    }
                })
                .defaultIfEmpty("")
                .last() // 获取最终结果
                .flatMap(finalText -> {
                    if (!StringUtils.hasText(finalText)) {
                        return Mono.empty();
                    }

                    // 记录语音识别完成时间并计算用时
                    long sttEndTime = System.currentTimeMillis();
                    Long sttStartTime = sessionSttStartTimes.get(sessionId);
                    if (sttStartTime != null) {
                        double sttDuration = (sttEndTime - sttStartTime) / 1000.0;
                        logger.info("语音识别完成 - SessionId: {}, 用时: {}秒, 识别结果: \"{}\"",
                                sessionId, df.format(sttDuration), finalText);
                    }

                    // 记录模型回复开始时间
                    sessionLlmStartTimes.put(sessionId, System.currentTimeMillis());

                    // 初始化TTS时间记录Map
                    sessionTtsStartTimes.putIfAbsent(sessionId, new ConcurrentHashMap<>());

                    // 初始化完整回复内容
                    sessionFullResponses.put(sessionId, new StringBuilder());

                    // 设置会话为非监听状态，防止处理自己的声音
                    sessionManager.setListeningState(sessionId, false);

                    // 初始化句子计数器
                    sessionSentenceCounters.putIfAbsent(sessionId, new AtomicInteger(0));

                    // 初始化句子队列和处理标志
                    sessionSentenceQueues.putIfAbsent(sessionId, new ConcurrentLinkedQueue<>());
                    sessionProcessingFlags.putIfAbsent(sessionId, new AtomicBoolean(false));

                    // 初始化音频准备映射
                    sessionPreparedAudio.putIfAbsent(sessionId, new ConcurrentSkipListMap<>());
                    sessionAudioFutures.putIfAbsent(sessionId, new ConcurrentHashMap<>());

                    // 发送最终识别结果
                    return messageService.sendMessage(session, "stt", "final", finalText)
                            .then(Mono.fromRunnable(() -> {
                                // 使用句子切分处理流式响应
                                llmManager.chatStreamBySentence(device, finalText,
                                        (sentence, isStart, isEnd) -> {
                                            // 获取句子序列号
                                            int sentenceNumber = sessionSentenceCounters.get(sessionId)
                                                    .incrementAndGet();

                                            // 累加完整回复内容
                                            sessionFullResponses.get(sessionId).append(sentence);

                                            // 计算模型响应时间
                                            double modelResponseTime = 0.00;
                                            Long llmStartTime = sessionLlmStartTimes.get(sessionId);
                                            if (llmStartTime != null) {
                                                modelResponseTime = (System.currentTimeMillis() - llmStartTime)
                                                        / 1000.0;
                                            }

                                            // 记录TTS开始时间
                                            sessionTtsStartTimes.get(sessionId).put(sentenceNumber,
                                                    System.currentTimeMillis());

                                            // 立即开始准备音频，但不立即播放
                                            prepareAudioForSentence(
                                                    sessionId,
                                                    sentence,
                                                    finalTtsConfig,
                                                    device.getVoiceName(),
                                                    sentenceNumber,
                                                    modelResponseTime);

                                            // 将句子添加到处理队列，确保按顺序处理
                                            addSentenceToQueue(
                                                    session,
                                                    sessionId,
                                                    sentence,
                                                    isStart,
                                                    isEnd,
                                                    finalTtsConfig,
                                                    device.getVoiceName(),
                                                    sentenceNumber);
                                        });
                            }));
                })
                .onErrorResume(error -> {
                    logger.error("流式识别错误: {}", error.getMessage(), error);
                    return Mono.empty();
                })
                .subscribe();

        return Mono.empty();
    }

    /**
     * 准备句子的音频，但不立即播放
     */
    private void prepareAudioForSentence(
            String sessionId,
            String sentence,
            SysConfig ttsConfig,
            String voiceName,
            int sentenceNumber,
            double modelResponseTime) {

        // 创建一个CompletableFuture来跟踪TTS处理
        CompletableFuture<String> audioFuture = new CompletableFuture<>();

        // 将Future添加到会话的映射中
        sessionAudioFutures.get(sessionId).put(sentenceNumber, audioFuture);

        // 记录TTS开始时间
        long ttsStartTime = System.currentTimeMillis();

        // 异步生成音频
        CompletableFuture.runAsync(() -> {
            try {
                // 调用TTS服务生成音频
                String audioPath = ttsService.getTtsService(ttsConfig, voiceName).textToSpeech(sentence);

                // 计算TTS处理用时（仅当前句子）
                long ttsDuration = System.currentTimeMillis() - ttsStartTime;

                // 使用统一格式记录日志：序号、模型响应时间、语音生成时间、内容
                logger.info("序号: {}, 模型回复: {}秒, 语音生成: {}秒, 内容: \"{}\"",
                        sentenceNumber, df.format(modelResponseTime), df.format(ttsDuration / 1000.0), sentence);

                // 将生成的音频路径存储到已准备好的映射中
                sessionPreparedAudio.get(sessionId).put(sentenceNumber, audioPath);

                // 完成Future
                audioFuture.complete(audioPath);
            } catch (Exception e) {
                logger.error("准备音频失败 - 句子序号: {}, 错误: {}", sentenceNumber, e.getMessage(), e);
                audioFuture.completeExceptionally(e);
            }
        });
    }

    /**
     * 将句子添加到处理队列并启动处理
     */
    private void addSentenceToQueue(
            WebSocketSession session,
            String sessionId,
            String sentence,
            boolean isStart,
            boolean isEnd,
            SysConfig ttsConfig,
            String voiceName,
            int sentenceNumber) {

        // 创建句子任务
        SentenceTask task = new SentenceTask(sentence, isStart, isEnd, ttsConfig, voiceName, sentenceNumber);

        // 获取会话的句子队列
        Queue<SentenceTask> queue = sessionSentenceQueues.get(sessionId);
        if (queue == null) {
            queue = new ConcurrentLinkedQueue<>();
            sessionSentenceQueues.put(sessionId, queue);
        }

        // 将任务添加到队列
        queue.add(task);

        // 如果当前没有正在处理的任务，开始处理队列
        AtomicBoolean isProcessing = sessionProcessingFlags.get(sessionId);
        if (isProcessing != null && isProcessing.compareAndSet(false, true)) {
            processSentenceQueue(session, sessionId);
        }
    }

    /**
     * 处理句子队列
     */
    private void processSentenceQueue(WebSocketSession session, String sessionId) {
        Queue<SentenceTask> queue = sessionSentenceQueues.get(sessionId);
        AtomicBoolean isProcessing = sessionProcessingFlags.get(sessionId);

        // 如果队列为空或会话已关闭，结束处理
        if (queue == null || queue.isEmpty() || !session.isOpen()) {
            if (isProcessing != null) {
                isProcessing.set(false);
            }
            return;
        }

        // 获取下一个任务
        SentenceTask task = queue.poll();

        // 获取该句子的音频Future
        CompletableFuture<String> audioFuture = sessionAudioFutures.getOrDefault(sessionId, new ConcurrentHashMap<>())
                .get(task.sentenceNumber);

        if (audioFuture == null) {
            // 如果没有找到音频Future，创建一个新的
            audioFuture = new CompletableFuture<>();

            // 计算模型响应时间
            double modelResponseTime = 0.00;
            Long llmStartTime = sessionLlmStartTimes.get(sessionId);
            if (llmStartTime != null) {
                modelResponseTime = (System.currentTimeMillis() - llmStartTime) / 1000.0;
            }

            prepareAudioForSentence(
                    sessionId,
                    task.sentence,
                    task.ttsConfig,
                    task.voiceName,
                    task.sentenceNumber,
                    modelResponseTime);
        }

        // 等待音频准备完成，然后发送
        audioFuture.whenComplete((audioPath, exception) -> {
            if (exception != null) {
                logger.error("获取音频失败 - 句子序号: {}, 错误: {}", task.sentenceNumber, exception.getMessage(), exception);

                // 继续处理队列中的下一个任务，即使当前任务失败
                if (!queue.isEmpty() && session.isOpen()) {
                    processSentenceQueue(session, sessionId);
                } else {
                    isProcessing.set(false);
                }
                return;
            }

            // 发送音频消息
            audioService.sendAudioMessage(session, audioPath, task.sentence, task.isStart, task.isEnd)
                    .doOnError(e -> logger.error("发送音频消息失败: {}", e.getMessage(), e))
                    .doFinally(signalType -> {
                        // 如果是最后一个句子，记录完整回复
                        if (task.isEnd) {
                            StringBuilder fullResponse = sessionFullResponses.get(sessionId);
                            if (fullResponse != null) {
                                logger.info("对话完成 - SessionId: {}, 完整回复: \"{}\"", sessionId,
                                        fullResponse.toString());
                            }
                        }

                        // 继续处理队列中的下一个任务
                        if (!queue.isEmpty() && session.isOpen()) {
                            processSentenceQueue(session, sessionId);
                        } else {
                            // 队列为空，重置处理状态
                            isProcessing.set(false);
                        }
                    })
                    .subscribe();
        });
    }

    /**
     * 处理语音唤醒
     */
    public Mono<Void> handleWakeWord(WebSocketSession session, String text) {
        String sessionId = session.getId();
        SysDevice device = sessionManager.getDeviceConfig(sessionId);

        if (device == null) {
            return Mono.empty();
        }

        // 获取配置并创建final变量以在lambda中使用
        final SysConfig ttsConfig = device.getTtsId() != null ? sessionManager.getCachedConfig(device.getTtsId())
                : null;
        sessionManager.updateLastActivity(sessionId);
        logger.info("检测到唤醒词: \"{}\"", text);

        // 记录模型回复开始时间
        sessionLlmStartTimes.put(sessionId, System.currentTimeMillis());

        // 初始化TTS时间记录Map
        sessionTtsStartTimes.putIfAbsent(sessionId, new ConcurrentHashMap<>());

        // 初始化完整回复内容
        sessionFullResponses.put(sessionId, new StringBuilder());

        // 设置为非监听状态，防止处理自己的声音
        sessionManager.setListeningState(sessionId, false);

        // 初始化句子计数器
        sessionSentenceCounters.putIfAbsent(sessionId, new AtomicInteger(0));

        // 初始化句子队列和处理标志
        sessionSentenceQueues.putIfAbsent(sessionId, new ConcurrentLinkedQueue<>());
        sessionProcessingFlags.putIfAbsent(sessionId, new AtomicBoolean(false));

        // 初始化音频准备映射
        sessionPreparedAudio.putIfAbsent(sessionId, new ConcurrentSkipListMap<>());
        sessionAudioFutures.putIfAbsent(sessionId, new ConcurrentHashMap<>());

        // 发送识别结果
        messageService.sendMessage(session, "stt", "start", text).subscribe();

        // 使用句子切分处理流式响应
        return Mono.fromRunnable(() -> {
            // 使用句子切分处理流式响应
            llmManager.chatStreamBySentence(device, text,
                    (sentence, isStart, isEnd) -> {
                        // 获取句子序列号
                        int sentenceNumber = sessionSentenceCounters.get(sessionId).incrementAndGet();

                        // 累加完整回复内容
                        sessionFullResponses.get(sessionId).append(sentence);

                        // 计算模型响应时间
                        double modelResponseTime = 0.00;
                        Long llmStartTime = sessionLlmStartTimes.get(sessionId);
                        if (llmStartTime != null) {
                            modelResponseTime = (System.currentTimeMillis() - llmStartTime) / 1000.0;
                        }

                        // 记录TTS开始时间
                        sessionTtsStartTimes.get(sessionId).put(sentenceNumber, System.currentTimeMillis());

                        // 立即开始准备音频，但不立即播放
                        prepareAudioForSentence(
                                sessionId,
                                sentence,
                                ttsConfig,
                                device.getVoiceName(),
                                sentenceNumber,
                                modelResponseTime);

                        // 将句子添加到处理队列，确保按顺序处理
                        addSentenceToQueue(
                                session,
                                sessionId,
                                sentence,
                                isStart,
                                isEnd,
                                ttsConfig,
                                device.getVoiceName(),
                                sentenceNumber);
                    });
        }).subscribeOn(Schedulers.boundedElastic()).then();
    }

    /**
     * 中止当前对话
     * 
     * @param session WebSocket会话
     * @param reason  中止原因
     * @return 处理结果
     */
    public Mono<Void> abortDialogue(WebSocketSession session, String reason) {
        String sessionId = session.getId();
        logger.info("中止对话 - SessionId: {}, Reason: {}", sessionId, reason);

        // 关闭音频流
        sessionManager.closeAudioSink(sessionId);
        sessionManager.setStreamingState(sessionId, false);

        // 清空句子队列
        Queue<SentenceTask> queue = sessionSentenceQueues.get(sessionId);
        if (queue != null) {
            queue.clear();
        }

        // 清空音频准备映射
        Map<Integer, CompletableFuture<String>> futures = sessionAudioFutures.get(sessionId);
        if (futures != null) {
            futures.clear();
        }

        ConcurrentSkipListMap<Integer, String> preparedAudio = sessionPreparedAudio.get(sessionId);
        if (preparedAudio != null) {
            preparedAudio.clear();
        }

        // 终止语音发送
        return audioService.sendStop(session);
    }

    /**
     * 清理会话资源
     * 
     * @param sessionId 会话ID
     */
    public void cleanupSession(String sessionId) {
        sessionSentenceCounters.remove(sessionId);

        // 移除句子队列和处理标志
        sessionSentenceQueues.remove(sessionId);
        sessionProcessingFlags.remove(sessionId);

        // 移除时间记录
        sessionSttStartTimes.remove(sessionId);
        sessionLlmStartTimes.remove(sessionId);
        sessionTtsStartTimes.remove(sessionId);
        sessionFullResponses.remove(sessionId);

        // 移除音频准备映射
        sessionPreparedAudio.remove(sessionId);
        sessionAudioFutures.remove(sessionId);
    }
}