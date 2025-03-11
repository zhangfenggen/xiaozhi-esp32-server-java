package com.xiaozhi;

import io.github.whitemagic2014.tts.TTS;
import io.github.whitemagic2014.tts.TTSVoice;
import io.github.whitemagic2014.tts.bean.Voice;

import java.util.stream.Collectors;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;


@SpringBootTest
@ActiveProfiles("test")
public class ApplicationTest {

    @Test
    public void testTTS() {
        // Voice can be found in file "voicesList.json"
        Voice voice = TTSVoice.provides().stream().filter(v -> v.getShortName().equals("zh-CN-XiaoyiNeural"))
                .collect(Collectors.toList()).get(0);
        String content = "你好，有什么可以帮助你的吗";
        String fileName = new TTS(voice, content)
                .findHeadHook()
                .isRateLimited(true) // Set to true to resolve the rate limiting issue in certain regions..
                .overwrite(false) // When the specified file name is the same, it will either overwrite or append
                                  // to the file.
                .formatMp3() // default mp3.
                .trans();

        System.out.println("Generated audio file: " + fileName);
    }

}