package com.xiaozhi.websocket.service;

import org.vosk.Model;
import org.vosk.Recognizer;
import org.bytedeco.ffmpeg.global.avcodec;
import org.bytedeco.ffmpeg.global.avutil;
import org.bytedeco.javacv.FFmpegFrameRecorder;
import org.bytedeco.javacv.Frame;
import org.bytedeco.javacv.FrameRecorder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.UUID;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;

import org.springframework.stereotype.Service;

import com.xiaozhi.utils.DateUtils;

@Service
public class SpeechToTextService {

    private static final Logger logger = LoggerFactory.getLogger(SpeechToTextService.class);

    private Model model;

    private final String filePath = "audio/";

    /**
     * 初始化 Vosk 模型，只加载一次
     */
    @PostConstruct
    public void init() {
        try {
            // 加载模型，路径为 resources 目录下的模型
            String modelPath = "src/main/resources/vosk-model-cn-0.22";
            model = new Model(modelPath);
            logger.info("Vosk 模型加载成功！路径: {}", modelPath);
        } catch (Exception e) {
            logger.error("Vosk 模型加载失败！", e);
            throw new RuntimeException("无法加载 Vosk 模型", e);
        }
    }

    /**
     * 将音频字节数组转换为文本
     *
     * @param audioData 完整的音频字节数组
     * @return 识别的文本结果
     */
    public String processAudio(byte[] audioData) {
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

            // 将原始音频数据转换为WAV格式并保存
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

    /**
     * 将原始音频数据保存为WAV文件
     * 
     * @param audioData  原始音频数据
     * @param outputFile 输出WAV文件
     * @throws IOException 如果保存过程中发生IO错误
     */
    private void saveAsWavFile(byte[] audioData, File outputFile) throws IOException {
        // WAV文件参数
        int sampleRate = 16000; // 与Recognizer使用的采样率保持一致
        int channels = 1; // 单声道
        int bitsPerSample = 16; // 16位采样

        try (FileOutputStream fos = new FileOutputStream(outputFile);
                DataOutputStream dos = new DataOutputStream(fos)) {

            // 写入WAV文件头
            // RIFF头
            dos.writeBytes("RIFF");
            dos.writeInt(Integer.reverseBytes(36 + audioData.length)); // 文件长度
            dos.writeBytes("WAVE");

            // fmt子块
            dos.writeBytes("fmt ");
            dos.writeInt(Integer.reverseBytes(16)); // 子块大小
            dos.writeShort(Short.reverseBytes((short) 1)); // 音频格式 (1 = PCM)
            dos.writeShort(Short.reverseBytes((short) channels)); // 通道数
            dos.writeInt(Integer.reverseBytes(sampleRate)); // 采样率
            dos.writeInt(Integer.reverseBytes(sampleRate * channels * bitsPerSample / 8)); // 字节率
            dos.writeShort(Short.reverseBytes((short) (channels * bitsPerSample / 8))); // 块对齐
            dos.writeShort(Short.reverseBytes((short) bitsPerSample)); // 每个样本的位数

            // data子块
            dos.writeBytes("data");
            dos.writeInt(Integer.reverseBytes(audioData.length)); // 数据大小

            // 写入音频数据
            dos.write(audioData);
        }
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
            java.nio.ShortBuffer shortBuffer = java.nio.ShortBuffer.wrap(samples);

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

    /**
     * 在应用关闭时释放模型资源
     */
    @PreDestroy
    public void cleanup() {
        if (model != null) {
            model.close();
            logger.info("Vosk 模型已释放！");
        }
    }
}
