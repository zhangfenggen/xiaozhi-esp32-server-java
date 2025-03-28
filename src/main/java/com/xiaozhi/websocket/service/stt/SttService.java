package com.xiaozhi.websocket.service.stt;

/**
 * Speech-to-Text服务接口
 * 定义了语音识别服务的基本操作
 */
public interface SttService {
    
    /**
     * 处理音频数据并转换为文本
     * 
     * @param audioData 音频数据字节数组
     * @return 识别的文本结果
     */
    String processAudio(byte[] audioData);
    
    /**
     * 获取提供商名称
     * 
     * @return 提供商名称
     */
    String getProviderName();
    
    /**
     * 初始化服务
     * 
     * @return 是否初始化成功
     */
    boolean initialize();
    
    /**
     * 清理资源
     */
    void cleanup();
    
    /**
     * 检查服务是否已初始化并可用
     * 
     * @return 服务是否可用
     */
    boolean isAvailable();
}