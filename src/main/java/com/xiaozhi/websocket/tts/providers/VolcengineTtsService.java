package com.xiaozhi.websocket.tts.providers;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.xiaozhi.entity.SysConfig;
import com.xiaozhi.websocket.tts.TtsService;

import java.io.File;
import java.io.FileOutputStream;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import net.sf.json.JSONObject;

import java.util.Base64;

public class VolcengineTtsService implements TtsService {
    private static final Logger logger = LoggerFactory.getLogger(VolcengineTtsService.class);

    private static final String PROVIDER_NAME = "volcengine";
    private static final String API_URL = "https://openspeech.bytedance.com/api/v1/tts";
    private static final MediaType JSON = MediaType.parse("application/json; charset=utf-8");

    // 音频名称
    private String voiceName;

    // 音频输出路径
    private String outputPath;

    // API相关
    private String appId;
    private String accessToken; // 对应 apiKey

    private final OkHttpClient client = new OkHttpClient.Builder()
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS)
            .build();

    public VolcengineTtsService(SysConfig config, String voiceName, String outputPath) {
        this.voiceName = voiceName;
        this.outputPath = outputPath;
        this.appId = config.getAppId();
        this.accessToken = config.getApiKey();
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
            // 生成音频文件名
            String audioFileName = getAudioFileName();
            String audioFilePath = outputPath + audioFileName;

            // 发送POST请求
            boolean success = sendRequest(text, audioFilePath);

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
     * 发送POST请求到火山引擎API，获取语音合成结果
     */
    private boolean sendRequest(String text, String audioFilePath) throws Exception {
        try {
            // 构建请求参数
            JSONObject requestJson = new JSONObject();

            // app部分
            JSONObject app = new JSONObject();
            app.put("appid", appId);
            app.put("token", accessToken);
            app.put("cluster", "volcano_tts");
            requestJson.put("app", app);

            // user部分
            JSONObject user = new JSONObject();
            user.put("uid", UUID.randomUUID().toString());
            requestJson.put("user", user);

            // audio部分
            JSONObject audio = new JSONObject();
            audio.put("voice_type", voiceName);
            audio.put("encoding", "mp3");
            audio.put("speed_ratio", 1.0);
            audio.put("volume_ratio", 1.0);
            audio.put("pitch_ratio", 1.0);
            requestJson.put("audio", audio);

            // request部分
            JSONObject request_JsonObject = new JSONObject();
            request_JsonObject.put("reqid", UUID.randomUUID().toString());
            request_JsonObject.put("text", text);
            request_JsonObject.put("text_type", "plain");
            request_JsonObject.put("operation", "query");
            request_JsonObject.put("with_frontend", 1);
            request_JsonObject.put("frontend_type", "unitTson");
            requestJson.put("request", request_JsonObject);

            // 使用Bearer Token鉴权方式
            String bearerToken = "Bearer; " + accessToken; // 注意分号是火山引擎的特殊格式

            RequestBody requestBody = RequestBody.create(JSON, requestJson.toString());

            // 设置请求头和请求体
            Request request = new Request.Builder()
                    .url(API_URL)
                    .addHeader("Content-Type", "application/json")
                    .addHeader("Authorization", bearerToken) // 添加Authorization头
                    .post(requestBody)
                    .build();

            try (Response response = client.newCall(request).execute()) {
                if (!response.isSuccessful()) {
                    String errorBody = response.body() != null ? response.body().string() : "无响应体";
                    logger.error("TTS请求失败: {} {}, 错误信息: {}", response.code(), response.message(), errorBody);
                    return false;
                }

                // 解析响应
                if (response.body() != null) {
                    String responseBody = response.body().string();
                    JSONObject jsonResponse = JSONObject.fromObject(responseBody);

                    // 检查响应是否包含错误
                    if (jsonResponse.containsKey("code") && jsonResponse.getInt("code") != 3000) {
                        logger.error("TTS请求返回错误: code={}, message={}",
                                jsonResponse.getInt("code"),
                                jsonResponse.getString("message"));
                        return false;
                    }

                    // 获取音频数据
                    if (jsonResponse.containsKey("data")) {
                        String base64Audio = jsonResponse.getString("data");
                        byte[] audioData = Base64.getDecoder().decode(base64Audio);

                        // 确保目录存在
                        File audioFileDir = new File(outputPath);
                        if (!audioFileDir.exists()) {
                            audioFileDir.mkdirs();
                        }

                        // 保存音频文件
                        File audioFile = new File(audioFilePath);
                        try (FileOutputStream fout = new FileOutputStream(audioFile)) {
                            fout.write(audioData);
                        }

                        return true;
                    } else {
                        logger.error("TTS响应中未找到音频数据: {}", responseBody);
                        return false;
                    }
                } else {
                    logger.error("TTS响应体为空");
                    return false;
                }
            }
        } catch (Exception e) {
            logger.error("发送TTS请求时发生错误", e);
            throw new Exception("发送TTS请求失败", e);
        }
    }
}
