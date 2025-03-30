package com.xiaozhi.websocket.stt;

/**
 * STT服务接口
 */
public interface SttService {

  /**
   * 获取服务提供商名称
   */
  String getProviderName();

  /**
   * 处理音频数据
   * 
   * @param audioData 音频字节数组
   * @return 识别的文本结果
   */
  String recognition(byte[] audioData);

}
