package com.xiaozhi.websocket.tts.providers;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.JsonObject;
import com.xiaozhi.entity.SysConfig;
import com.xiaozhi.utils.AliyunAccessToken;
import com.xiaozhi.websocket.tts.TtsService;

import java.io.File;
import java.io.FileOutputStream;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class AliyunTtsService implements TtsService {
    private static final Logger logger = LoggerFactory.getLogger(AliyunTtsService.class);

    private static final String PROVIDER_NAME = "aliyun";
    private static final String API_URL = "https://nls-gateway.aliyuncs.com/stream/v1/tts";
    private static final MediaType JSON = MediaType.parse("application/json; charset=utf-8");

    // 音频名称
    private String voiceName;

    // 音频输出路径
    private String outputPath;

    // Token相关
    private String appKey;
    private String accessKeyId;
    private String accessKeySecret;
    private String token;
    private String tokenExpireTime;

    private final OkHttpClient client = new OkHttpClient.Builder()
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS)
            .build();

    public AliyunTtsService(SysConfig config, String voiceName, String outputPath) {
        this.voiceName = voiceName;
        this.outputPath = outputPath;
        this.appKey = config.getApiKey();
        this.accessKeyId = config.getAppId();
        this.accessKeySecret = config.getApiSecret();
    }

    @Override
    public String getProviderName() {
        return PROVIDER_NAME;
    }

    @Override
    public String getAudioFileName() {
        String uuid = UUID.randomUUID().toString().replace("-", "");
        return uuid + ".mp3";
    }

    @Override
    public String textToSpeech(String text) throws Exception {
        if (text == null || text.isEmpty()) {
            logger.warn("文本内容为空！");
            return null;
        }

        try {
            // 获取有效token
            String nlsToken = getValidToken();
            if (nlsToken == null) {
                logger.error("无法获取有效的阿里云NLS Token");
                throw new Exception("无法获取阿里云NLS Token");
            }

            // 生成音频文件名
            String audioFileName = getAudioFileName();
            String audioFilePath = outputPath + audioFileName;
            
            // 发送POST请求
            boolean success = sendRequest(text, nlsToken, audioFilePath);
            
            if (success) {
                return audioFilePath;
            } else {
                throw new Exception("语音合成失败");
            }
        } catch (Exception e) {
            logger.error("语音合成时发生错误！", e);
            throw e;
        }
    }
    
    /**
     * 发送POST请求到阿里云API，获取语音合成结果
     */
    private boolean sendRequest(String text, String nlsToken, String audioFilePath) throws Exception {
        try {
            // 构建JSON请求体
            JsonObject requestJson = new JsonObject();
            requestJson.addProperty("appkey", appKey);
            requestJson.addProperty("text", text);
            requestJson.addProperty("token", nlsToken);
            requestJson.addProperty("format", "mp3");
            requestJson.addProperty("sample_rate", 16000);
            requestJson.addProperty("voice", voiceName);
            
            RequestBody requestBody = RequestBody.create(JSON, requestJson.toString());
            
            // 设置请求头和请求体
            Request request = new Request.Builder()
                    .url(API_URL)
                    .addHeader("Content-Type", "application/json")
                    .post(requestBody)
                    .build();

            try (Response response = client.newCall(request).execute()) {
                
                if (!response.isSuccessful()) {
                    String errorBody = response.body() != null ? response.body().string() : "无响应体";
                    logger.error("TTS请求失败: {} {}, 错误信息: {}", response.code(), response.message(), errorBody);
                    return false;
                }
                
                String contentType = response.header("Content-Type");
                if (contentType != null && contentType.contains("audio/")) {
                    // 确保目录存在
                    File audioFileDir = new File(outputPath);
                    if (!audioFileDir.exists()) {
                        audioFileDir.mkdirs();
                    }
                    
                    // 保存音频文件
                    File audioFile = new File(audioFilePath);
                    try (FileOutputStream fout = new FileOutputStream(audioFile)) {
                        if (response.body() != null) {
                            fout.write(response.body().bytes());
                        } else {
                            logger.error("TTS响应体为空");
                            return false;
                        }
                    }
                    
                    return true;
                } else {
                    // 处理错误响应
                    String errorMessage = response.body() != null ? response.body().string() : "未知错误";
                    logger.error("TTS请求失败，非音频响应: {}", errorMessage);
                    return false;
                }
            }
        } catch (Exception e) {
            logger.error("发送TTS请求时发生错误", e);
            throw new Exception("发送TTS请求失败", e);
        }
    }

    /**
     * 获取有效的阿里云NLS Token
     */
    private String getValidToken() {
        // 检查当前token是否存在且未过期
        if (token != null && tokenExpireTime != null) {
            try {
                // 阿里云返回的过期时间是Unix时间戳（秒）
                long expireTimeInSeconds = Long.parseLong(tokenExpireTime);
                long currentTimeInSeconds = System.currentTimeMillis() / 1000;

                // 如果token还有效（未过期），直接返回
                if (expireTimeInSeconds > currentTimeInSeconds) {
                    return token;
                }
            } catch (NumberFormatException e) {
                logger.warn("解析token过期时间出错，将重新获取token", e);
            }
        }

        // 获取新token
        try {
            Map<String, String> tokenInfo = AliyunAccessToken.createToken(accessKeyId, accessKeySecret);

            if (tokenInfo != null && tokenInfo.containsKey("token") && tokenInfo.containsKey("expireTime")) {
                token = tokenInfo.get("token");
                tokenExpireTime = tokenInfo.get("expireTime");
                return token;
            } else {
                logger.error("获取阿里云NLS Token失败");
                return null;
            }
        } catch (Exception e) {
            logger.error("获取阿里云NLS Token时发生错误", e);
            return null;
        }
    }
}