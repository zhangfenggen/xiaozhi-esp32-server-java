package com.xiaozhi.websocket.stt.providers;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.text.SimpleDateFormat;
import java.util.Base64;
import java.util.Date;
import java.util.SimpleTimeZone;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.xiaozhi.entity.SysConfig;
import com.xiaozhi.utils.AudioUtils;
import com.xiaozhi.utils.DateUtils;
import com.xiaozhi.websocket.stt.SttService;

import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class AliyunSttService implements SttService {
    private static final Logger logger = LoggerFactory.getLogger(AliyunSttService.class);

    private static final String PROVIDER_NAME = "aliyun";
    private static final String API_URL = "https://nls-gateway-cn-shanghai.aliyuncs.com/stream/v1/asr";
    private static final MediaType JSON = MediaType.parse("application/json; charset=utf-8");

    // 阿里云一句话识别的请求格式
    private static final String FORMAT = "pcm"; // 支持的音频格式：pcm, wav, mp3等
    private static final int SAMPLE_RATE = 16000; // 采样率，支持8000或16000

    private String appKey; // 阿里云语音识别需要appKey

    private final OkHttpClient client = new OkHttpClient.Builder()
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS)
            .build();

    private final ObjectMapper objectMapper = new ObjectMapper();

    // 构造函数，接收配置对象
    public AliyunSttService(SysConfig config) {
        if (config != null) {
            this.appKey = config.getApiKey();
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

        // 将原始音频数据转换为WAV格式并保存（用于调试）
        String fileName = AudioUtils.saveAsWavFile(audioData);

        try {
            // 检查配置是否已设置
            if (appKey == null) {
                logger.error("阿里云语音识别配置未设置，无法进行识别");
                return null;
            }

            // 将音频数据转换为Base64编码
            String base64Audio = Base64.getEncoder().encodeToString(audioData);

            // 构建请求体
            String requestBody = buildRequestBody(base64Audio);

            // 计算签名并发送请求
            String result = sendRequest(requestBody);

            return result;
        } catch (Exception e) {
            logger.error("处理音频时发生错误！", e);
            return null;
        }
    }

    /**
     * 构建请求体
     */
    private String buildRequestBody(String base64Audio) throws Exception {
        // 构建请求参数
        ObjectNode requestMap = objectMapper.createObjectNode();

        // 必需参数
        requestMap.put("format", FORMAT);
        requestMap.put("sample_rate", SAMPLE_RATE);
        requestMap.put("appkey", appKey);

        // 音频数据直接放在请求体中，而不是使用speech字段
        requestMap.put("audio", base64Audio);

        // 可选参数
        requestMap.put("enable_punctuation_prediction", true);
        requestMap.put("enable_inverse_text_normalization", true);

        return objectMapper.writeValueAsString(requestMap);
    }

    /**
     * 发送请求到阿里云API
     */
    private String sendRequest(String requestBody) throws IOException {
        try {
            // 构建请求
            Request request = new Request.Builder()
                    .url(API_URL)
                    .addHeader("Content-Type", "application/octet-stream")
                    .addHeader("Host", "nls-gateway-cn-shanghai.aliyuncs.com")
                    .addHeader("x-nls-token", appKey)
                    .post(RequestBody.create(JSON, requestBody))
                    .build();

            logger.debug(request.toString());
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

}
