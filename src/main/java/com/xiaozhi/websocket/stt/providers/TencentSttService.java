package com.xiaozhi.websocket.stt.providers;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Base64;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.security.MessageDigest;

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

public class TencentSttService implements SttService {
    private static final Logger logger = LoggerFactory.getLogger(TencentSttService.class);

    private static final String PROVIDER_NAME = "tencent";
    private static final String API_URL = "https://asr.tencentcloudapi.com";
    private static final String API_VERSION = "2019-06-14";
    private static final MediaType JSON = MediaType.parse("application/json; charset=utf-8");
    private static final String FORMAT = "pcm"; // 支持的音频格式：pcm, wav, mp3

    private String secretId;
    private String secretKey;

    private final OkHttpClient client = new OkHttpClient.Builder()
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS)
            .build();

    private final ObjectMapper objectMapper = new ObjectMapper();

    // 构造函数，接收配置对象
    public TencentSttService(SysConfig config) {
        if (config != null) {
            this.secretId = config.getApiKey();
            this.secretKey = config.getApiSecret();
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

        // 将原始音频数据转换为MP3格式并保存（用于调试）
        String fileName = AudioUtils.saveAsMp3File(audioData);

        try {
            // 检查配置是否已设置
            if (secretId == null || secretKey == null) {
                logger.error("腾讯云语音识别配置未设置，无法进行识别");
                return null;
            }

            // 将音频数据转换为Base64编码
            String base64Audio = Base64.getEncoder().encodeToString(audioData);

            // 构建请求体
            String requestBody = buildRequestBody(base64Audio);

            // 获取认证头
            String[] authHeaders = getAuthHeaders(requestBody);

            // 发送请求
            String result = sendRequest(requestBody, authHeaders);

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
        requestMap.put("ProjectId", 0);
        requestMap.put("SubServiceType", 2); // 一句话识别
        requestMap.put("EngSerViceType", "16k_zh"); // 中文普通话通用
        requestMap.put("SourceType", 1); // 音频数据来源为语音文件
        requestMap.put("VoiceFormat", FORMAT); // 音频格式
        requestMap.put("Data", base64Audio); // Base64编码的音频数据
        requestMap.put("DataLen", base64Audio.length()); // 数据长度
        return objectMapper.writeValueAsString(requestMap);
    }

    /**
     * 获取认证头
     */
    private String[] getAuthHeaders(String requestBody) {
        try {
            // 获取当前UTC时间戳
            ZonedDateTime now = ZonedDateTime.now(ZoneId.of("UTC"));
            String timestamp = String.valueOf(now.toEpochSecond());
            String date = now.format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));

            // 服务名称必须是 "asr"
            String service = "asr";

            // 拼接凭证范围
            String credentialScope = date + "/" + service + "/tc3_request";

            // 使用TC3-HMAC-SHA256签名方法
            String algorithm = "TC3-HMAC-SHA256";

            // 构建规范请求字符串
            String httpRequestMethod = "POST";
            String canonicalUri = "/";
            String canonicalQueryString = "";

            // 注意：头部信息需要按照ASCII升序排列，且key和value都转为小写
            // 必须包含content-type和host头部
            String contentType = "application/json; charset=utf-8";
            String host = "asr.tencentcloudapi.com";
            String action = "SentenceRecognition"; // 接口名称

            // 构建规范头部信息，注意顺序和格式
            String canonicalHeaders = "content-type:" + contentType.toLowerCase() + "\n" +
                    "host:" + host.toLowerCase() + "\n" +
                    "x-tc-action:" + action.toLowerCase() + "\n";

            String signedHeaders = "content-type;host;x-tc-action";

            // 请求体哈希值
            String payloadHash = sha256Hex(requestBody);

            // 构建规范请求字符串
            String canonicalRequest = httpRequestMethod + "\n" +
                    canonicalUri + "\n" +
                    canonicalQueryString + "\n" +
                    canonicalHeaders + "\n" +
                    signedHeaders + "\n" +
                    payloadHash;

            // 计算规范请求的哈希值
            String hashedCanonicalRequest = sha256Hex(canonicalRequest);

            // 构建待签名字符串
            String stringToSign = algorithm + "\n" +
                    timestamp + "\n" +
                    credentialScope + "\n" +
                    hashedCanonicalRequest;

            // 计算签名密钥
            byte[] secretDate = hmacSha256("TC3" + secretKey, date);
            byte[] secretService = hmacSha256(secretDate, service);
            byte[] secretSigning = hmacSha256(secretService, "tc3_request");

            // 计算签名
            String signature = bytesToHex(hmacSha256(secretSigning, stringToSign));

            // 构建授权头
            String authorization = algorithm + " " +
                    "Credential=" + secretId + "/" + credentialScope + ", " +
                    "SignedHeaders=" + signedHeaders + ", " +
                    "Signature=" + signature;

            return new String[] {
                    timestamp,
                    authorization
            };
        } catch (Exception e) {
            logger.error("生成认证头失败", e);
            throw new RuntimeException("生成认证头失败", e);
        }
    }

    /**
     * 发送请求到腾讯云API
     */
    private String sendRequest(String requestBody, String[] authHeaders) throws IOException {
        String timestamp = authHeaders[0];
        String authorization = authHeaders[1];

        Request request = new Request.Builder()
                .url(API_URL)
                .addHeader("Content-Type", "application/json; charset=utf-8")
                .addHeader("Host", "asr.tencentcloudapi.com")
                .addHeader("Authorization", authorization)
                .addHeader("X-TC-Action", "SentenceRecognition")
                .addHeader("X-TC-Version", API_VERSION)
                .addHeader("X-TC-Timestamp", timestamp)
                .addHeader("X-TC-Region", "ap-shanghai")
                .post(RequestBody.create(JSON, requestBody))
                .build();

        try (Response response = client.newCall(request).execute()) {
            if (!response.isSuccessful()) {
                throw new IOException("请求失败: " + response.code() + " " + response.message());
            }

            String responseBody = response.body().string();
            JsonNode jsonNode = objectMapper.readTree(responseBody);

            // 检查是否有错误
            if (jsonNode.has("Response") && jsonNode.get("Response").has("Error")) {
                JsonNode error = jsonNode.get("Response").get("Error");
                String errorCode = error.get("Code").asText();
                String errorMessage = error.get("Message").asText();
                throw new IOException("API返回错误: " + errorCode + ": " + errorMessage);
            }

            // 提取识别结果
            if (jsonNode.has("Response") && jsonNode.get("Response").has("Result")) {
                return jsonNode.get("Response").get("Result").asText();
            } else {
                logger.warn("响应中没有识别结果: {}", responseBody);
                return "";
            }
        }
    }

    /**
     * 计算字符串的SHA256哈希值
     */
    private String sha256Hex(String data) throws NoSuchAlgorithmException {
        MessageDigest digest = MessageDigest.getInstance("SHA-256");
        byte[] hash = digest.digest(data.getBytes(StandardCharsets.UTF_8));
        return bytesToHex(hash);
    }

    /**
     * 计算HMAC-SHA256
     */
    private byte[] hmacSha256(String key, String data) throws NoSuchAlgorithmException, InvalidKeyException {
        return hmacSha256(key.getBytes(StandardCharsets.UTF_8), data);
    }

    /**
     * 计算HMAC-SHA256
     */
    private byte[] hmacSha256(byte[] key, String data) throws NoSuchAlgorithmException, InvalidKeyException {
        String algorithm = "HmacSHA256";
        Mac mac = Mac.getInstance(algorithm);
        mac.init(new SecretKeySpec(key, algorithm));
        return mac.doFinal(data.getBytes(StandardCharsets.UTF_8));
    }

    /**
     * 字节数组转十六进制字符串
     */
    private String bytesToHex(byte[] bytes) {
        StringBuilder sb = new StringBuilder();
        for (byte b : bytes) {
            sb.append(String.format("%02x", b & 0xff));
        }
        return sb.toString();
    }
}