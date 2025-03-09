package com.xiaozhi.websocket.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.xiaozhi.websocket.WebSocketHandler;
import com.xiaozhi.websocket.service.MessageService;

import io.github.jaredmdobson.concentus.OpusEncoder;
import io.github.jaredmdobson.concentus.OpusApplication;
import io.github.jaredmdobson.concentus.OpusException;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

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

    // 帧持续时间（毫秒）
    private static final int FRAME_DURATION_MS = 60;

    @Autowired
    @Qualifier("webSocketMessageService")
    private MessageService messageService;

    @Autowired
    private TextToSpeechService textToSpeechService;

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
        List<byte[]> opusFrames = convertToOpus(pcmData, sampleRate, channels);

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
     * 将PCM数据转换为Opus格式
     */
    private List<byte[]> convertToOpus(byte[] pcmData, int sampleRate, int channels) throws OpusException {
        // 创建Opus编码器
        OpusEncoder encoder = new OpusEncoder(sampleRate, channels, OpusApplication.OPUS_APPLICATION_VOIP);

        // 设置比特率 (16kbps)
        encoder.setBitrate(16000);

        // 每帧样本数 (60ms帧长)
        int frameSize = sampleRate * FRAME_DURATION_MS / 1000;

        // 处理PCM数据
        List<byte[]> opusFrames = new ArrayList<>();
        short[] shortBuffer = new short[frameSize * channels];

        for (int i = 0; i < pcmData.length / 2; i += frameSize * channels) {
            // 将字节数据转换为short
            for (int j = 0; j < frameSize * channels && (i + j) < pcmData.length / 2; j++) {
                int byteIndex = (i + j) * 2;
                if (byteIndex + 1 < pcmData.length) {
                    shortBuffer[j] = (short) ((pcmData[byteIndex] & 0xFF) | (pcmData[byteIndex + 1] << 8));
                }
            }

            // 编码
            byte[] opusBuffer = new byte[1275]; // 最大Opus帧大小
            int opusLength = encoder.encode(shortBuffer, 0, frameSize, opusBuffer, 0, opusBuffer.length);

            // 创建正确大小的帧并添加到列表
            byte[] opusFrame = new byte[opusLength];
            System.arraycopy(opusBuffer, 0, opusFrame, 0, opusLength);
            opusFrames.add(opusFrame);
        }

        return opusFrames;
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