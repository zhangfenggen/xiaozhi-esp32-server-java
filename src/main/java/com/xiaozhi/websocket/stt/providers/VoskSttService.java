package com.xiaozhi.websocket.stt.providers;

import java.io.ByteArrayInputStream;

import javax.annotation.PostConstruct;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.vosk.LibVosk;
import org.vosk.LogLevel;
import org.vosk.Model;
import org.vosk.Recognizer;

import com.xiaozhi.utils.AudioUtils;
import com.xiaozhi.websocket.stt.SttService;

/**
 * Vosk STT服务实现
 */
public class VoskSttService implements SttService {

  private static final Logger logger = LoggerFactory.getLogger(VoskSttService.class);
  private static final String PROVIDER_NAME = "vosk";

  // Vosk模型相关对象
  private Model model;
  private String voskModelPath;

  /**
   * 初始化Vosk模型
   */
  @PostConstruct
  public void initialize() throws Exception {
    try {
      // 禁用Vosk日志输出
      LibVosk.setLogLevel(LogLevel.WARNINGS);

      // 加载模型，路径为配置的模型目录
      voskModelPath = System.getProperty("user.dir") + "/models/vosk-model";
      logger.debug(voskModelPath);
      model = new Model(voskModelPath);
      logger.info("Vosk 模型加载成功！路径: {}", voskModelPath);
    } catch (Exception e) {
      logger.warn("Vosk 模型加载失败！将使用其他STT服务: {}", e.getMessage());
    }

    logger.info("Vosk STT服务初始化完成");
  }

  @Override
  public String getProviderName() {
    return PROVIDER_NAME;
  }

  @Override
  public String recognition(byte[] audioData) {
    if (audioData == null || audioData.length == 0) {
      logger.warn("音频数据为空！");
      return null;
    }

    // 将原始音频数据转换为MP3格式并保存
    String fileName = AudioUtils.saveAsMp3File(audioData);

    try (Recognizer recognizer = new Recognizer(model, 16000)) { // 16000 是采样率
      ByteArrayInputStream audioStream = new ByteArrayInputStream(audioData);

      byte[] buffer = new byte[4096];
      int bytesRead;

      while ((bytesRead = audioStream.read(buffer)) != -1) {
        if (recognizer.acceptWaveForm(buffer, bytesRead)) {
          // 如果识别到完整的结果
          return recognizer.getResult();
        }
      }

      // 返回最终的识别结果
      return recognizer.getFinalResult();

    } catch (Exception e) {
      logger.error("处理音频时发生错误！", e);
      return null;
    }
  }

}
