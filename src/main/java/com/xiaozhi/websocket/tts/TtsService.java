package com.xiaozhi.websocket.tts;

/**
 * TTS服务接口
 */
public interface TtsService {

  /**
   * 获取服务提供商名称
   */
  String getProviderName();

  /**
   * 生成文件名称
   * 
   * @return 文件名称
   */
  String getAudioFileName();

  /**
   * 将文本转换为语音（带自定义语音）
   * 
   * @param text 要转换为语音的文本
   * @return 生成的音频文件路径
   */
  String textToSpeech(String text) throws Exception;

}