package com.xiaozhi.websocket.tts.providers;

import io.github.whitemagic2014.tts.TTS;
import io.github.whitemagic2014.tts.TTSVoice;
import io.github.whitemagic2014.tts.bean.Voice;

import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.UUID;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.xiaozhi.utils.AudioUtils;
import com.xiaozhi.websocket.tts.TtsService;

public class EdgeTtsService implements TtsService {
    private static final Logger logger = LoggerFactory.getLogger(EdgeTtsService.class);

    private static final String PROVIDER_NAME = "edge";

    // 音频名称
    private String voiceName;

    // 音频输出路径
    private String outputPath;

    public EdgeTtsService(String voiceName, String outputPath) {
        this.voiceName = voiceName;
        this.outputPath = outputPath;
    }

    @Override
    public String getProviderName() {
        return PROVIDER_NAME;
    }

    @Override
    public String getAudioFileName() {
        String uuid = UUID.randomUUID().toString().replace("-", "");
        return uuid + ".opus";
    }

    @Override
    public String textToSpeech(String text) throws Exception {
        // 获取中文语音
        Voice voiceObj = TTSVoice.provides().stream()
                .filter(v -> v.getShortName().equals(voiceName))
                .collect(Collectors.toList()).get(0);

        TTS ttsEngine = new TTS(voiceObj, text);
        // 执行TTS转换获取音频文件
        String audioFilePath = ttsEngine.findHeadHook()
                .storage(outputPath)
                .fileName(getAudioFileName().split("\\.")[0])
                .isRateLimited(true)
                .overwrite(false)
                .formatMp3()
                .trans();

        String fullPath = outputPath + audioFilePath;

        // 1. 将MP3转换为PCM (已经设置为16kHz采样率和单声道)
        byte[] pcmData = AudioUtils.mp3ToPcm(fullPath);

        // 2. 将PCM转换回WAV (使用AudioUtils中的设置：16kHz, 单声道, 160kbps)
        String resampledFileName = AudioUtils.saveAsWav(pcmData);

        // 3. 删除原始文件
        Files.deleteIfExists(Paths.get(fullPath));

        // 4. 返回重采样后的文件路径
        return AudioUtils.AUDIO_PATH + resampledFileName;
    }

    @Override
    public void streamTextToSpeech(String text, Consumer<byte[]> audioDataConsumer) throws Exception {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'streamTextToSpeech'");
    }
}