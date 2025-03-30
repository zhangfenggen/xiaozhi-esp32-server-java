package com.xiaozhi.websocket.tts.providers;

import io.github.whitemagic2014.tts.TTS;
import io.github.whitemagic2014.tts.TTSVoice;
import io.github.whitemagic2014.tts.bean.Voice;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.xiaozhi.entity.SysConfig;
import com.xiaozhi.websocket.tts.TtsService;

public class EdgeTtsService implements TtsService {
    private static final Logger logger = LoggerFactory.getLogger(EdgeTtsService.class);
    
    private static final String PROVIDER_NAME = "edge";
    private boolean isRateLimited = true;

    // 语音生成文件保存地址
    private final String filePath = "audio/";

    // 默认语音
    private static final String DEFAULT_VOICE = "zh-CN-XiaoyiNeural";
    
    // 配置信息
    private SysConfig config;
    
    public EdgeTtsService() {
        // 默认构造函数
    }
    
    public EdgeTtsService(SysConfig config) {
        this.config = config;
    }

    @Override
    public String getProviderName() {
        return PROVIDER_NAME;
    }

    @Override
    public String textToSpeech(String text) throws Exception {
        return textToSpeech(text, DEFAULT_VOICE, 16000, 1);
    }

    @Override
    public String textToSpeech(String text, String voiceName) throws Exception {
        return textToSpeech(text, voiceName, 16000, 1);
    }

    @Override
    public String textToSpeech(String text, String voiceName, int sampleRate, int channels) throws Exception {
        // 如果未指定语音名称，使用默认语音
        String voice = voiceName != null && !voiceName.isEmpty() ? voiceName : DEFAULT_VOICE;
        
        // 获取中文语音
        Voice voiceObj = TTSVoice.provides().stream()
                .filter(v -> v.getShortName().equals(voice))
                .collect(Collectors.toList()).get(0);

        // 1. 执行TTS转换获取音频文件
        String audioFilePath = convertTextToSpeech(voiceObj, text);

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

        try {
            // 先尝试删除原始文件
            if (originalFile.exists()) {
                if (!originalFile.delete()) {
                    // 如果删除失败，可能是文件被占用
                    // 使用Java NIO的Files.copy方法，它支持覆盖选项
                    Files.copy(tempFile.toPath(), originalFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
                    // 复制成功后删除临时文件
                    tempFile.delete();
                } else {
                    // 原始文件删除成功，重命名临时文件
                    if (!tempFile.renameTo(originalFile)) {
                        throw new RuntimeException("文件重命名失败");
                    }
                }
            } else {
                // 原始文件不存在，直接重命名
                if (!tempFile.renameTo(originalFile)) {
                    throw new RuntimeException("文件重命名失败");
                }
            }
        } catch (IOException e) {
            logger.error("文件操作失败: " + e.getMessage(), e);
            throw new RuntimeException("文件操作失败", e);
        }
    }
}