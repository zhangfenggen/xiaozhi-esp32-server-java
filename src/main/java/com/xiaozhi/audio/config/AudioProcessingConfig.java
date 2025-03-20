package com.xiaozhi.audio.config;

import com.xiaozhi.audio.detector.impl.SileroVadDetector;
import com.xiaozhi.audio.processor.TarsosNoiseReducer;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;

@Configuration
public class AudioProcessingConfig {

    @Autowired
    private SileroVadDetector sileroVadDetector;

    @Autowired(required = false)
    private TarsosNoiseReducer tarsosNoiseReducer;

    @EventListener(ApplicationReadyEvent.class)
    public void setupAudioProcessing() {
        // 默认启用TarsosDSP降噪
        if (tarsosNoiseReducer != null) {
            sileroVadDetector.setEnableNoiseReduction(true);

            // 配置TarsosDSP降噪参数
            tarsosNoiseReducer.setSpectralSubtractionFactor(1.5);
            tarsosNoiseReducer.setNoiseEstimationFrames(15);
        }
    }
}