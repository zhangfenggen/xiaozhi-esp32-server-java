package com.xiaozhi.websocket.tts.factory;

import com.xiaozhi.entity.SysConfig;
import com.xiaozhi.websocket.tts.TtsService;
import com.xiaozhi.websocket.tts.providers.EdgeTtsService;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class TtsServiceFactory {

  private static final Logger logger = LoggerFactory.getLogger(TtsServiceFactory.class);

  // 缓存已初始化的服务
  private final Map<String, TtsService> serviceCache = new ConcurrentHashMap<>();

  // 默认服务提供商名称
  private static final String DEFAULT_PROVIDER = "edge";

  /**
   * 获取默认TTS服务
   */
  public TtsService getDefaultTtsService() {
    // 如果缓存中没有默认服务，则创建一个
    if (!serviceCache.containsKey(DEFAULT_PROVIDER)) {
      TtsService edgeService = new EdgeTtsService();
      serviceCache.put(DEFAULT_PROVIDER, edgeService);
      logger.info("创建默认Edge TTS服务");
    }
    
    return serviceCache.get(DEFAULT_PROVIDER);
  }

  /**
   * 根据配置获取TTS服务
   */
  public TtsService getTtsService(SysConfig config) {
    if (config == null) {
      return getDefaultTtsService();
    }

    String provider = config.getProvider();
    
    // 如果是默认提供商且尚未初始化，则初始化
    if (DEFAULT_PROVIDER.equals(provider) && !serviceCache.containsKey(provider)) {
      TtsService edgeService = new EdgeTtsService(config);
      serviceCache.put(DEFAULT_PROVIDER, edgeService);
      return edgeService;
    }

    // 检查是否已有该提供商的服务实例
    if (serviceCache.containsKey(provider)) {
      return serviceCache.get(provider);
    }

    // 创建新的服务实例
    try {
      TtsService service;
      if (DEFAULT_PROVIDER.equals(provider)) {
        service = new EdgeTtsService(config);
      } else {
        // 创建其他API服务
        service = createApiService(config);
      }
      
      if (service != null) {
        serviceCache.put(provider, service);
      }
      
      return service;
    } catch (Exception e) {
      logger.error("创建{}服务失败", provider, e);
      return getDefaultTtsService(); // 失败时返回默认服务
    }
  }

  /**
   * 根据配置创建API类型的TTS服务
   */
  private TtsService createApiService(SysConfig config) {
    if (config == null) {
      return null;
    }

    String provider = config.getProvider();
    logger.info("创建 {} TTS服务", provider);

    // 根据提供商类型创建对应的服务实例
    // 这里可以添加其他TTS服务提供商的支持
    // 例如：阿里云、腾讯云等
    
    // 如果是Edge，直接返回Edge服务
    if (DEFAULT_PROVIDER.equals(provider)) {
      return new EdgeTtsService(config);
    }
    
    // 未来可以在这里添加其他服务提供商的实现
    // if ("aliyun".equals(provider)) {
    //   return new AliyunTtsService(config);
    // } else if ("tencent".equals(provider)) {
    //   return new TencentTtsService(config);
    // }

    logger.warn("不支持的TTS服务提供商: {}", provider);
    return null;
  }
}