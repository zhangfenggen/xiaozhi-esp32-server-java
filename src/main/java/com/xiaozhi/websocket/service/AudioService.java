package com.xiaozhi.websocket.service;

import com.xiaozhi.audio.detector.VadDetector;
import com.xiaozhi.audio.processor.OpusProcessor;
import com.xiaozhi.websocket.handler.WebSocketHandler;

import java.io.File;
import java.io.FileInputStream;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.web.socket.BinaryMessage;
import org.springframework.web.socket.WebSocketSession;

@Service
public class AudioService {

    private static final Logger logger = LoggerFactory.getLogger(WebSocketHandler.class);

    // 播放帧持续时间（毫秒）
    private static final int FRAME_DURATION_MS = 60;

    @Autowired
    @Qualifier("WebSocketMessageService")
    private MessageService messageService;

    @Autowired
    private OpusProcessor opusProcessor;

    @Autowired
    private VadDetector vadDetector;

    /**
     * 音频处理结果类
     */
    public static class AudioProcessResult {
        private List<byte[]> opusFrames;
        private long durationMs;

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
     * 清理会话的音频处理状态
     */
    public void cleanupSession(String sessionId) {
        vadDetector.resetSession(sessionId);
    }

    /**
     * 处理音频文件，提取PCM数据并转换为Opus格式
     * 
     * @param audioFilePath 音频文件路径
     * @param sampleRate    采样率
     * @param channels      通道数
     * @return 处理结果，包含opus数据和持续时间
     */
    public AudioProcessResult processAudioFile(String audioFilePath, int sampleRate, int channels) throws Exception {
        // 从MP3获取PCM数据
        byte[] pcmData = extractPcmFromAudio(audioFilePath);
        long durationMs = calculateDuration(pcmData, sampleRate, channels);

        // 转换为Opus格式
        List<byte[]> opusFrames = opusProcessor.convertPcmToOpus(pcmData, sampleRate, channels, FRAME_DURATION_MS);

        return new AudioProcessResult(opusFrames, durationMs);
    }

    /**
     * 从音频文件提取PCM数据
     */
    private byte[] extractPcmFromAudio(String audioFilePath) throws Exception {
        // 创建临时PCM文件
        File tempPcmFile = File.createTempFile("temp_pcm_extract_", ".pcm");
        String tempPcmPath = tempPcmFile.getAbsolutePath();
        try {
            // 使用FFmpeg直接将MP3转换为PCM
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
                throw new RuntimeException("PCM数据提取失败");
            }

            // 读取PCM文件内容
            File pcmFile = new File(tempPcmPath);
            byte[] pcmData = new byte[(int) pcmFile.length()];
            try (FileInputStream fis = new FileInputStream(pcmFile)) {
                fis.read(pcmData);
            }

            return pcmData;
        } finally {
            // 删除临时文件
            tempPcmFile.delete();
        }
    }

    /**
     * 计算音频持续时间（毫秒）
     */
    private long calculateDuration(byte[] pcmData, int sampleRate, int channels) {
        // PCM 16位数据，每个样本2字节
        int bytesPerSample = 2;
        int totalSamples = pcmData.length / (bytesPerSample * channels);
        return (long) (totalSamples * 1000.0 / sampleRate);
    }

    /**
     * 简化版本的发送音频方法
     */
    public void sendAudio(WebSocketSession session, String audioFilePath, String text) throws Exception {
        sendAudio(session, audioFilePath, text, true, true, true);
    }

    /**
     * 分段发送音频数据
     * 
     * @param session       WebSocket会话
     * @param audioFilePath 音频文件路径
     * @param text          文本内容
     * @param isFirstText   是否是第一段文本
     * @param isLastText    是否是最后一段文本
     * @param llmFinishTask LLM是否已完成任务
     */
    public void sendAudio(WebSocketSession session, String audioFilePath, String text,
            boolean isFirstText, boolean isLastText, boolean llmFinishTask) throws Exception {

        if (session == null || !session.isOpen()) {
            logger.warn("尝试发送音频到已关闭或空的WebSocket会话");
            return;
        }

        try {
            // 处理音频文件，获取Opus帧和持续时间
            AudioProcessResult audioData = processAudioFile(audioFilePath, 16000, 1);

            // 发送句子开始消息
            if (isFirstText) {
                logger.info("发送第一段语音: {} ,音频时长: {} 秒", text, audioData.getDurationMs() / 1000);
            }

            // 发送控制消息
            sendSentenceStart(session, text);
            Thread.sleep(20);
            sendStart(session);

            // 获取Opus帧列表
            List<byte[]> opusFrames = audioData.getOpusFrames();

            // 开始发送音频帧
            long startTime = System.nanoTime();
            long playPosition = 0;

            for (int i = 0; i < opusFrames.size(); i++) {
                // 如果会话已关闭，停止发送
                if (!session.isOpen()) {
                    logger.info("会话已关闭，停止发送音频");
                    break;
                }

                byte[] frame = opusFrames.get(i);

                // 计算预期发送时间（纳秒）
                long expectedTime = startTime + (playPosition * 1_000_000);
                long currentTime = System.nanoTime();
                long delayNanos = expectedTime - currentTime;

                // 执行延迟
                if (delayNanos > 0) {
                    // 转换为毫秒和纳秒余数
                    long delayMillis = delayNanos / 1_000_000;
                    int delayNanosRemainder = (int) (delayNanos % 1_000_000);

                    if (delayMillis > 0 || delayNanosRemainder > 0) {
                        Thread.sleep(delayMillis, delayNanosRemainder);
                    }
                }

                // 再次检查会话是否仍然打开
                if (session.isOpen()) {
                    // 直接发送Opus帧
                    session.sendMessage(new BinaryMessage(frame));

                    // 更新播放位置（毫秒）
                    playPosition += FRAME_DURATION_MS;
                } else {
                    logger.info("会话已关闭，停止发送音频");
                    break;
                }
            }

            Thread.sleep(10);
            // 发送句子结束消息
            if (session.isOpen()) {
                sendSentenceEnd(session, text);

                // 如果是最后一个文本且LLM已完成任务，发送停止消息
                if (llmFinishTask && isLastText) {
                    sendStop(session);
                }
            }

        } catch (Exception e) {
            logger.error("发送音频数据时发生错误", e);
            throw e;
        }
    }

    // 发送TTS句子开始指令（包含文本）
    public void sendSentenceStart(WebSocketSession session, String text) {
        messageService.sendMessage(session, "tts", "sentence_start", text);
    }

    // 发送TTS句子结束指令
    public void sendSentenceEnd(WebSocketSession session, String text) {
        messageService.sendMessage(session, "tts", "sentence_end", text);
    }

    // 发送TTS开始指令
    public void sendStart(WebSocketSession session) {
        messageService.sendMessage(session, "tts", "start");
    }

    // 发送TTS停止指令
    public void sendStop(WebSocketSession session) {
        messageService.sendMessage(session, "tts", "stop");
    }
}