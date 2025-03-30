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
   * 将文本转换为语音
   * 
   * @param text 要转换为语音的文本
   * @return 生成的音频文件路径
   */
  String textToSpeech(String text) throws Exception;
  
  /**
   * 将文本转换为语音（带自定义语音）
   * 
   * @param text 要转换为语音的文本
   * @param voiceName 语音名称
   * @return 生成的音频文件路径
   */
  String textToSpeech(String text, String voiceName) throws Exception;
  
  /**
   * 将文本转换为语音（带自定义参数）
   * 
   * @param text 要转换为语音的文本
   * @param voiceName 语音名称
   * @param sampleRate 采样率
   * @param channels 通道数
   * @return 生成的音频文件路径
   */
  String textToSpeech(String text, String voiceName, int sampleRate, int channels) throws Exception;
}