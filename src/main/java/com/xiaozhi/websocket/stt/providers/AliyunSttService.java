package com.xiaozhi.websocket.stt.providers;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.xiaozhi.entity.SysConfig;
import com.xiaozhi.utils.AliyunAccessToken;
import com.xiaozhi.websocket.stt.SttService;

import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class AliyunSttService implements SttService {
    private static final Logger logger = LoggerFactory.getLogger(AliyunSttService.class);

    private static final String PROVIDER_NAME = "aliyun";
    private static final String API_URL = "https://nls-gateway.aliyuncs.com/stream/v1/asr";

    // 音频格式设置
    private static final String FORMAT = "pcm";
    private static final int SAMPLE_RATE = 16000;

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

    private final ObjectMapper objectMapper = new ObjectMapper();

    public AliyunSttService(SysConfig config) {
        if (config != null) {
            this.appKey = config.getApiKey();
            this.accessKeyId = config.getAppId();
            this.accessKeySecret = config.getApiSecret();
        }
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

        try {
            // 检查配置
            if (appKey == null || accessKeyId == null || accessKeySecret == null) {
                logger.error("阿里云语音识别配置未设置，无法进行识别");
                return null;
            }

            // 获取有效token
            String nlsToken = getValidToken();
            if (nlsToken == null) {
                logger.error("无法获取有效的阿里云NLS Token");
                return null;
            }

            // 构建URL，添加所有必要的参数
            String url = buildRequestUrl();

            // 直接发送音频数据
            String result = sendRequest(url, nlsToken, audioData);

            return result;
        } catch (Exception e) {
            logger.error("处理音频时发生错误！", e);
            return null;
        }
    }

    /**
     * 构建请求URL，添加所有必要的查询参数
     */
    private String buildRequestUrl() {
        StringBuilder urlBuilder = new StringBuilder(API_URL);
        urlBuilder.append("?appkey=").append(appKey);
        urlBuilder.append("&format=").append(FORMAT);
        urlBuilder.append("&sample_rate=").append(SAMPLE_RATE);
        urlBuilder.append("&enable_punctuation_prediction=").append(true);
        urlBuilder.append("&enable_inverse_text_normalization=").append(true);

        return urlBuilder.toString();
    }

    /**
     * 发送请求到阿里云API，直接传输音频数据
     */
    private String sendRequest(String url, String nlsToken, byte[] audioData) throws IOException {
        try {
            // 设置请求头
            HashMap<String, String> headers = new HashMap<>();
            headers.put("X-NLS-Token", nlsToken);
            headers.put("Content-Type", "application/octet-stream");

            // 直接使用音频字节数据创建请求体
            RequestBody requestBody = RequestBody.create(MediaType.parse("application/octet-stream"), audioData);

            Request.Builder requestBuilder = new Request.Builder()
                    .url(url);

            // 添加所有头部信息
            for (Map.Entry<String, String> entry : headers.entrySet()) {
                requestBuilder.addHeader(entry.getKey(), entry.getValue());
            }

            // 设置POST请求体
            Request request = requestBuilder.post(requestBody).build();

            try (Response response = client.newCall(request).execute()) {
                if (!response.isSuccessful()) {
                    throw new IOException("请求失败: " + response.code() + " " + response.message());
                }

                String responseBody = response.body().string();
                JsonNode jsonNode = objectMapper.readTree(responseBody);

                // 检查是否有错误
                if (jsonNode.has("status") && jsonNode.get("status").asInt() != 20000000) {
                    String errorCode = jsonNode.get("status").asText();
                    String errorMessage = jsonNode.has("message") ? jsonNode.get("message").asText() : "未知错误";
                    throw new IOException("API返回错误: " + errorCode + ": " + errorMessage);
                }

                // 提取识别结果
                if (jsonNode.has("result")) {
                    return jsonNode.get("result").asText();
                } else {
                    logger.warn("响应中没有识别结果: {}", responseBody);
                    return "";
                }
            }
        } catch (Exception e) {
            logger.error("发送请求时发生错误", e);
            throw new IOException("发送请求失败", e);
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
                    logger.debug("使用缓存的token，过期时间: {}", tokenExpireTime);
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