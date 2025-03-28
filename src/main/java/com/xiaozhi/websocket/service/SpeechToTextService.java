package com.xiaozhi.websocket.service;

import com.xiaozhi.websocket.service.stt.SttService;
import com.xiaozhi.websocket.service.stt.SttServiceFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import java.io.File;
import java.io.IOException;
@Service
public class SpeechToTextService {

    private static final Logger logger = LoggerFactory.getLogger(SpeechToTextService.class);

    @Autowired
    private SttServiceFactory sttServiceFactory;
    /**
     * 初始化STT服务
     */
    @PostConstruct
    public void init() {
        try {
            // 初始化所有STT服务
            sttServiceFactory.initializeServices();
            logger.info("STT服务初始化完成");
        } catch (Exception e) {
            // 捕获异常但不阻止应用启动
            logger.error("STT服务初始化失败，但应用将继续运行", e);
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

        // 使用默认的STT服务处理音频
        SttService sttService = sttServiceFactory.getDefaultSttService();
        logger.info("使用STT服务: {}", sttService.getProviderName());
        
        return sttService.processAudio(audioData);
    }
    /**
     * 使用指定的STT服务处理音频
     *
     * @param audioData 完整的音频字节数组
     * @param provider  STT服务提供商
     * @return 识别的文本结果
     */
    public String processAudio(byte[] audioData, String provider) {
        if (audioData == null || audioData.length == 0) {
            logger.warn("音频数据为空！");
            return null;
        }

        // 使用指定的STT服务处理音频
        SttService sttService = sttServiceFactory.getSttService(provider);
        logger.info("使用STT服务: {}", sttService.getProviderName());
        
        return sttService.processAudio(audioData);
    }
    /**
     * 使用指定配置ID的STT服务处理音频
     *
     * @param audioData 完整的音频字节数组
     * @param configId  配置ID
     * @return 识别的文本结果
     */
    public String processAudioWithConfig(byte[] audioData, Integer configId) {
        if (audioData == null || audioData.length == 0) {
            logger.warn("音频数据为空！");
            return null;
        }

        // 使用指定配置的STT服务处理音频
        SttService sttService = sttServiceFactory.getSttServiceByConfigId(configId);
        logger.info("使用STT服务: {}", sttService.getProviderName());
        
        return sttService.processAudio(audioData);
    }
    /**
     * 在应用关闭时释放资源
     */
    @PreDestroy
    public void cleanup() {
        logger.info("清理STT服务资源");
    }
}
