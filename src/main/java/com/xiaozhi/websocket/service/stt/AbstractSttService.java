package com.xiaozhi.websocket.service.stt;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.UUID;

/**
 * STT服务的抽象基类
 * 提供了一些通用的功能实现
 */
public abstract class AbstractSttService implements SttService {
    
    private static final Logger logger = LoggerFactory.getLogger(AbstractSttService.class);
    
    protected final String filePath = "audio/";
    protected boolean available = false;
    
    /**
     * 保存音频文件到指定目录
     * 
     * @param audioData 音频数据
     * @return 保存的文件路径，如果保存失败则返回null
     */
    protected String saveAudioFile(byte[] audioData) {
        if (audioData == null || audioData.length == 0) {
            logger.warn("音频数据为空！");
            return null;
        }

        try {
            // 确保存储目录存在
            File storageDir = new File(filePath);
            if (!storageDir.exists()) {
                storageDir.mkdirs();
            }

            String fileName = UUID.randomUUID().toString().replace("-", "");
            File outputFile = new File(storageDir, fileName + ".mp3");
            
            // 保存音频文件
            try (FileOutputStream fos = new FileOutputStream(outputFile)) {
                fos.write(audioData);
            }
            
            logger.info("音频文件已保存至: {}", outputFile.getAbsolutePath());
            return outputFile.getAbsolutePath();
        } catch (IOException e) {
            logger.error("保存音频文件时发生错误！", e);
            return null;
        }
    }
    
    @Override
    public boolean isAvailable() {
        return available;
    }
    
    /**
     * 从字节数组创建输入流
     * 
     * @param data 字节数组
     * @return 字节数组输入流
     */
    protected ByteArrayInputStream createInputStream(byte[] data) {
        return new ByteArrayInputStream(data);
    }
}