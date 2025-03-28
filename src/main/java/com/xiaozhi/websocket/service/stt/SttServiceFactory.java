package com.xiaozhi.websocket.service.stt;

import com.xiaozhi.entity.SysConfig;
import com.xiaozhi.service.SysConfigService;
import com.xiaozhi.websocket.service.stt.impl.FallbackSttService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

/**
 * STT服务工厂类
 * 负责创建和管理不同的STT服务实例
 */
@Component
public class SttServiceFactory {

    private static final Logger logger = LoggerFactory.getLogger(SttServiceFactory.class);

    @Autowired
    private ApplicationContext applicationContext;

    @Autowired
    private SysConfigService configService;

    @Autowired
    private FallbackSttService fallbackSttService;

    private final Map<String, SttService> sttServices = new HashMap<>();
    private SttService defaultSttService;

    /**
     * 初始化STT服务
     * 只初始化本地Vosk服务，其他服务按需初始化
     */
    public void initializeServices() {
        logger.info("开始初始化STT服务...");

        // 首先初始化备用服务
        fallbackSttService.initialize();
        sttServices.put("fallback", fallbackSttService);
        defaultSttService = fallbackSttService;

        // 尝试初始化Vosk服务（本地服务）
        try {
            String beanName = "voskSttService";
            if (applicationContext.containsBean(beanName)) {
                SttService voskService = (SttService) applicationContext.getBean(beanName);
                boolean initialized = voskService.initialize();

                if (initialized) {
                    sttServices.put("vosk", voskService);
                    defaultSttService = voskService; // 设置Vosk为默认服务
                    logger.info("成功初始化Vosk STT服务");
                } else {
                    logger.warn("Vosk STT服务初始化失败，将使用备用服务");
                }
            } else {
                logger.warn("未找到Vosk STT服务实现，将使用备用服务");
            }
        } catch (Exception e) {
            logger.error("初始化Vosk STT服务时出错", e);
        }

        logger.info("STT服务初始化完成，默认服务: {}", defaultSttService.getProviderName());
    }

    /**
     * 获取STT服务
     * 如果服务不存在或未初始化，则尝试按需初始化
     * 
     * @param provider 提供商名称
     * @return 对应的STT服务，如果不存在则返回默认服务
     */
    public SttService getSttService(String provider) {
        if (provider == null || provider.isEmpty()) {
            return defaultSttService;
        }

        String providerLower = provider.toLowerCase();
        SttService service = sttServices.get(providerLower);

        // 如果服务不存在，尝试按需初始化
        if (service == null) {
            service = initializeServiceOnDemand(provider);
        }

        return service != null ? service : defaultSttService;
    }

    /**
     * 按需初始化STT服务
     * 
     * @param provider 提供商名称
     * @return 初始化的服务，如果初始化失败则返回null
     */
    private SttService initializeServiceOnDemand(String provider) {
        String beanName = provider.toLowerCase() + "SttService";

        try {
            if (applicationContext.containsBean(beanName)) {
                logger.info("按需初始化STT服务: {}", provider);
                SttService sttService = (SttService) applicationContext.getBean(beanName);
                boolean initialized = sttService.initialize();

                if (initialized) {
                    sttServices.put(provider.toLowerCase(), sttService);
                    logger.info("成功初始化STT服务: {}", provider);
                    return sttService;
                } else {
                    logger.warn("STT服务初始化失败: {}", provider);
                }
            } else {
                logger.warn("未找到STT服务实现: {}", beanName);
            }
        } catch (Exception e) {
            logger.error("初始化STT服务时出错: " + provider, e);
        }

        return null;
    }

    /**
     * 获取默认的STT服务
     * 
     * @return 默认的STT服务
     */
    public SttService getDefaultSttService() {
        return defaultSttService;
    }

    /**
     * 根据配置ID获取STT服务
     * 
     * @param configId 配置ID
     * @return 对应的STT服务，如果不存在则返回默认服务
     */
    public SttService getSttServiceByConfigId(Integer configId) {
        if (configId == null) {
            return defaultSttService;
        }

        SysConfig config = configService.selectConfigById(configId);
        if (config == null || !"stt".equals(config.getConfigType())) {
            return defaultSttService;
        }

        return getSttService(config.getProvider());
    }
}