package com.xiaozhi.websocket.service.stt.impl;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.xiaozhi.entity.SysConfig;
import com.xiaozhi.service.SysConfigService;
import com.xiaozhi.websocket.service.stt.AbstractSttService;
import okhttp3.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.TimeUnit;

/**
 * 基于阿里云智能语音交互服务的语音识别实现
 */
@Service("aliyunSttService")
public class AliyunSttService extends AbstractSttService {

    private static final Logger logger = LoggerFactory.getLogger(AliyunSttService.class);
    private static final String API_URL = "https://nls-gateway.cn-shanghai.aliyuncs.com/stream/v1/asr";
    private static final String HOST = "nls-gateway.cn-shanghai.aliyuncs.com";
    private static final String HTTP_METHOD = "POST";
    private static final String HTTP_URI = "/stream/v1/asr";
    private static final String CONTENT_TYPE = "application/octet-stream";
    private static final String ALGORITHM = "HmacSHA1";

    private final OkHttpClient client = new OkHttpClient.Builder()
            .connectTimeout(10, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS)
            .build();

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Autowired
    private SysConfigService configService;

    private String accessKeyId;
    private String accessKeySecret;
    private String appKey;

    @Override
    public boolean initialize() {
        try {
            // 从配置中获取阿里云语音识别服务的配置
            SysConfig queryConfig = new SysConfig();
            queryConfig.setConfigType("stt");
            queryConfig.setProvider("aliyun");
            List<SysConfig> configs = configService.query(queryConfig);

            if (configs == null || configs.isEmpty()) {
                logger.warn("未找到阿里云STT服务配置");
                return false;
            }

            SysConfig config = configs.get(0);
            this.accessKeyId = config.getApiKey();
            this.accessKeySecret = config.getApiSecret();
            this.appKey = config.getAppId();

            // 检查配置是否完整
            if (accessKeyId == null || accessKeyId.isEmpty() || 
                accessKeySecret == null || accessKeySecret.isEmpty() ||
                appKey == null || appKey.isEmpty()) {
                logger.warn("阿里云STT服务配置不完整");
                return false;
            }

            logger.info("阿里云STT服务初始化成功");
            available = true;
            return true;
        } catch (Exception e) {
            logger.error("阿里云STT服务初始化失败", e);
            available = false;
            return false;
        }
    }

    @Override
    public String processAudio(byte[] audioData) {
        if (!isAvailable()) {
            logger.error("阿里云STT服务不可用");
            return null;
        }

        try {
            // 保存音频文件以便日志记录
            String audioFilePath = saveAudioFile(audioData);
            if (audioFilePath == null) {
                return null;
            }

            // 构建请求参数
            String requestUrl = buildRequestUrl();
            
            // 计算签名
            String date = getGMTDate();
            String authorization = calculateAuthorization(date);

            // 构建请求头
            Headers headers = new Headers.Builder()
                    .add("Host", HOST)
                    .add("Content-Type", CONTENT_TYPE)
                    .add("Date", date)
                    .add("Authorization", authorization)
                    .build();

            // 构建请求体
            RequestBody requestBody = RequestBody.create(audioData, MediaType.parse(CONTENT_TYPE));

            // 构建请求
            Request request = new Request.Builder()
                    .url(requestUrl)
                    .headers(headers)
                    .post(requestBody)
                    .build();

            // 发送请求
            logger.info("发送阿里云STT请求: {}", requestUrl);
            try (Response response = client.newCall(request).execute()) {
                if (!response.isSuccessful()) {
                    logger.error("阿里云STT请求失败: {}", response.code());
                    return null;
                }

                String responseBody = response.body().string();
                logger.debug("阿里云STT响应: {}", responseBody);

                // 解析响应
                JsonNode jsonNode = objectMapper.readTree(responseBody);
                if (jsonNode.has("result") && !jsonNode.get("result").isNull()) {
                    String text = jsonNode.get("result").asText();
                    return "{\"text\":\"" + text + "\"}";
                } else if (jsonNode.has("status") && jsonNode.get("status").asInt() != 20000000) {
                    logger.error("阿里云STT错误: {}", responseBody);
                    return null;
                }
                
                return "{\"text\":\"\"}";
            }
        } catch (Exception e) {
            logger.error("阿里云STT服务处理音频失败", e);
            return null;
        }
    }

    @Override
    public void cleanup() {
        logger.info("清理阿里云STT服务资源");
        available = false;
    }

    @Override
    public String getProviderName() {
        return "Aliyun";
    }

    /**
     * 构建请求URL
     */
    private String buildRequestUrl() {
        return API_URL + "?appkey=" + appKey;
    }

    /**
     * 获取GMT格式的日期
     */
    private String getGMTDate() {
        SimpleDateFormat sdf = new SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss z", Locale.US);
        sdf.setTimeZone(TimeZone.getTimeZone("GMT"));
        return sdf.format(new Date());
    }

    /**
     * 计算Authorization头
     */
    private String calculateAuthorization(String date) throws NoSuchAlgorithmException, InvalidKeyException {
        String stringToSign = HTTP_METHOD + "\n" +
                              "application/octet-stream\n" +
                              "\n" +
                              "application/octet-stream\n" +
                              date + "\n" +
                              HTTP_URI;

        Mac mac = Mac.getInstance(ALGORITHM);
        mac.init(new SecretKeySpec(accessKeySecret.getBytes(StandardCharsets.UTF_8), ALGORITHM));
        byte[] signData = mac.doFinal(stringToSign.getBytes(StandardCharsets.UTF_8));
        String signature = Base64.getEncoder().encodeToString(signData);

        return "acs " + accessKeyId + ":" + signature;
    }
}