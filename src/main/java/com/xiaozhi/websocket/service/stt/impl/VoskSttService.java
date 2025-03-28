package com.xiaozhi.websocket.service.stt.impl;

import com.xiaozhi.utils.DateUtils;
import com.xiaozhi.websocket.service.stt.AbstractSttService;
import org.bytedeco.ffmpeg.global.avcodec;
import org.bytedeco.ffmpeg.global.avutil;
import org.bytedeco.javacv.FFmpegFrameRecorder;
import org.bytedeco.javacv.Frame;
import org.bytedeco.javacv.FrameRecorder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.vosk.LibVosk;
import org.vosk.LogLevel;
import org.vosk.Model;
import org.vosk.Recognizer;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.nio.ShortBuffer;
import java.util.UUID;

/**
 * 基于Vosk的语音识别服务实现
 */
@Service("voskSttService")
public class VoskSttService extends AbstractSttService {

    private static final Logger logger = LoggerFactory.getLogger(VoskSttService.class);

    private Model model;

    @Value("${vosk.model.path}")
    private String voskModelPath;

    @Override
    public boolean initialize() {
        try {
            // 禁用Vosk日志输出
            LibVosk.setLogLevel(LogLevel.WARNINGS);
            
            // 加载模型，路径为配置的模型目录
            File modelDir = new File(voskModelPath);
            if (!modelDir.exists() || !modelDir.isDirectory()) {
                logger.warn("Vosk模型目录不存在: {}", voskModelPath);
                return false;
            }
            
            model = new Model(voskModelPath);
            logger.info("Vosk 模型加载成功！路径: {}", voskModelPath);
            available = true;
            return true;
        } catch (Exception e) {
            logger.warn("Vosk 模型加载失败！将使用其他STT服务: {}", e.getMessage());
            available = false;
            return false;
        }
    }

    @Override
    public String processAudio(byte[] audioData) {
        if (!isAvailable()) {
            logger.error("Vosk服务不可用，无法处理音频");
            return null;
        }

        if (audioData == null || audioData.length == 0) {
            logger.warn("音频数据为空！");
            return null;
        }

        // 保存音频文件到storage目录
        try {
            // 确保storage目录存在
            File storageDir = new File(filePath);
            if (!storageDir.exists()) {
                storageDir.mkdirs();
            }

            String fileName = UUID.randomUUID().toString().replace("-", "");

            // 将原始音频数据转换为MP3格式并保存
            saveAsMp3File(audioData, new File(storageDir, fileName + ".mp3"));
            logger.info("音频文件已保存至: {}", new File(storageDir, fileName + ".mp3").getAbsolutePath());
        } catch (IOException e) {
            logger.error("保存音频文件时发生错误！", e);
        }

        long startTime = System.currentTimeMillis(); // 记录开始时间

        try (Recognizer recognizer = new Recognizer(model, 16000)) { // 16000 是采样率
            ByteArrayInputStream audioStream = new ByteArrayInputStream(audioData);

            byte[] buffer = new byte[4096];
            int bytesRead;

            while ((bytesRead = audioStream.read(buffer)) != -1) {
                if (recognizer.acceptWaveForm(buffer, bytesRead)) {
                    // 如果识别到完整的结果
                    return recognizer.getResult();
                }
            }

            long endTime = System.currentTimeMillis(); // 记录结束时间

            double duration = DateUtils.deltaTime(startTime, endTime); // 计算处理音频的总时间
            logger.info("语音识别完成，耗时：{} 秒", duration);
            // 返回最终的识别结果
            return recognizer.getFinalResult();

        } catch (Exception e) {
            logger.error("处理音频时发生错误！", e);
            return null;
        }
    }

    @Override
    public void cleanup() {
        if (model != null) {
            model.close();
            logger.info("Vosk 模型已释放！");
            available = false;
        }
    }

    @Override
    public String getProviderName() {
        return "Vosk";
    }

    /**
     * 将PCM音频数据直接保存为MP3文件，不经过临时WAV文件
     * 
     * @param pcmData    PCM格式的音频数据
     * @param outputFile 输出MP3文件
     * @throws IOException 如果保存过程中发生IO错误
     */
    private void saveAsMp3File(byte[] pcmData, File outputFile) throws IOException {
        int sampleRate = 16000; // 采样率
        int channels = 1; // 单声道

        try {
            // 创建内存中的帧记录器，直接输出到MP3文件
            FFmpegFrameRecorder recorder = new FFmpegFrameRecorder(outputFile, channels);
            recorder.setAudioChannels(channels);
            recorder.setSampleRate(sampleRate);
            recorder.setAudioCodec(avcodec.AV_CODEC_ID_MP3);
            recorder.setAudioQuality(0); // 高质量
            recorder.setAudioBitrate(128000); // 128kbps比特率
            recorder.setSampleFormat(avutil.AV_SAMPLE_FMT_S16); // 16位PCM
            recorder.start();

            // 创建音频帧
            Frame audioFrame = new Frame();

            // 将PCM数据转换为短整型数组
            short[] samples = new short[pcmData.length / 2];
            ByteArrayInputStream bais = new ByteArrayInputStream(pcmData);
            for (int i = 0; i < samples.length; i++) {
                // 读取两个字节，组合成一个short (小端序)
                int low = bais.read() & 0xff;
                int high = bais.read() & 0xff;
                samples[i] = (short) (low | (high << 8));
            }

            // 创建ShortBuffer并填充数据
            ShortBuffer shortBuffer = ShortBuffer.wrap(samples);

            // 设置音频帧数据
            audioFrame.samples = new java.nio.Buffer[] { shortBuffer };
            audioFrame.sampleRate = sampleRate;
            audioFrame.audioChannels = channels;

            // 记录音频帧
            recorder.record(audioFrame);

            // 关闭记录器
            recorder.stop();
            recorder.release();
            recorder.close();

            logger.info("PCM数据已直接编码为MP3格式并保存");
        } catch (FrameRecorder.Exception e) {
            logger.error("编码MP3时发生错误", e);
            throw new IOException("无法将PCM数据编码为MP3: " + e.getMessage(), e);
        }
    }
}