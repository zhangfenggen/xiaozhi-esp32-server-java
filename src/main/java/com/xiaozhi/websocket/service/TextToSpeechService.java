package com.xiaozhi.websocket.service;

import io.github.whitemagic2014.tts.TTS;
import io.github.whitemagic2014.tts.TTSVoice;
import io.github.whitemagic2014.tts.bean.Voice;

import java.io.File;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.stream.Collectors;

@Service
public class TextToSpeechService {
    private static final Logger logger = LoggerFactory.getLogger(TextToSpeechService.class);
    private boolean isRateLimited = true;

    // 语音生成文件保存地址
    private final String filePath = "audio/";

    /**
     * 将文本转换为语音，生成MP3文件
     * 
     * @param message 要转换为语音的文本
     * @return 生成的MP3文件路径
     */
    public String textToSpeech(String message) throws Exception {
        return textToSpeech(message, 16000, 1);
    }

    /**
     * 将文本转换为语音，生成MP3文件（带自定义参数）
     * 
     * @param message    要转换为语音的文本
     * @param sampleRate 采样率
     * @param channels   通道数
     * @return 生成的MP3文件路径
     */
    public String textToSpeech(String message, int sampleRate, int channels) throws Exception {

        // 获取角色

        // 获取中文语音
        Voice voice = TTSVoice.provides().stream()
                .filter(v -> v.getShortName().equals("zh-CN-XiaoyiNeural"))
                .collect(Collectors.toList()).get(0);

        // 1. 执行TTS转换获取音频文件
        String audioFilePath = convertTextToSpeech(voice, message);

        // 2. 转换原始MP3为指定采样率和通道数的MP3
        convertAndSaveAudio(audioFilePath, sampleRate, channels);

        return audioFilePath;
    }

    /**
     * 将文本转换为语音
     */
    private String convertTextToSpeech(Voice voice, String message) {
        TTS ttsEngine = new TTS(voice, message);
        String fileName = ttsEngine.findHeadHook()
                .storage(filePath)
                .isRateLimited(isRateLimited)
                .overwrite(false)
                .formatMp3()
                .trans();

        return filePath + fileName;
    }

    /**
     * 使用FFmpeg将原始音频转换为指定采样率和通道数的MP3
     */
    private void convertAndSaveAudio(String audioFilePath, int sampleRate, int channels) throws Exception {
        // 创建临时文件路径
        String tempFilePath = audioFilePath + ".tmp";
        // 使用FFmpeg直接转换音频
        String[] command = {
                "ffmpeg",
                "-i", audioFilePath,
                "-ar", String.valueOf(sampleRate),
                "-ac", String.valueOf(channels),
                "-acodec", "libmp3lame",
                "-q:a", "2",
                "-f", "mp3",
                "-y", // 覆盖已存在的文件
                tempFilePath
        };

        ProcessBuilder pb = new ProcessBuilder(command);
        pb.redirectErrorStream(true);
        Process process = pb.start();

        // 等待进程完成
        int exitCode = process.waitFor();
        if (exitCode != 0) {
            logger.error("FFmpeg转换音频失败，退出码: {}", exitCode);
            throw new RuntimeException("音频转换失败");
        }

        // 用临时文件替换原始文件
        File originalFile = new File(audioFilePath);
        File tempFile = new File(tempFilePath);

        if (!tempFile.renameTo(originalFile)) {
            logger.error("无法用临时文件替换原始文件");
            throw new RuntimeException("文件覆盖失败");
        }
    }
}
