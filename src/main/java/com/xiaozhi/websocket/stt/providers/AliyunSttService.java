package com.xiaozhi.websocket.stt.providers;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

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
import okhttp3.WebSocket;
import okhttp3.WebSocketListener;
import okio.ByteString;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Sinks;

public class AliyunSttService implements SttService {
    private static final Logger logger = LoggerFactory.getLogger(AliyunSttService.class);

    private static final String PROVIDER_NAME = "aliyun";
    private static final String API_URL = "https://nls-gateway.aliyuncs.com/stream/v1/asr";
    private static final String WS_API_URL = "wss://nls-gateway.aliyuncs.com/ws/v1";

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

    // 存储当前活跃的WebSocket连接
    private final ConcurrentHashMap<String, WebSocket> activeWebSockets = new ConcurrentHashMap<>();

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
    public boolean supportsStreaming() {
        return true;
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

    @Override
    public Flux<String> streamRecognition(Flux<byte[]> audioStream) {
        // 检查配置是否已设置
        if (appKey == null || accessKeyId == null || accessKeySecret == null) {
            logger.error("阿里云语音识别配置未设置，无法进行识别");
            return Flux.error(new IllegalStateException("阿里云语音识别配置未设置"));
        }

        // 创建结果接收器
        Sinks.Many<String> resultSink = Sinks.many().multicast().onBackpressureBuffer();

        // 生成唯一的任务ID
        String taskId = UUID.randomUUID().toString();

        try {
            // 获取有效token
            String nlsToken = getValidToken();
            if (nlsToken == null) {
                logger.error("无法获取有效的阿里云NLS Token");
                return Flux.error(new IllegalStateException("无法获取有效的阿里云NLS Token"));
            }

            // 构建WebSocket连接URL
            String url = WS_API_URL;

            // 创建WebSocket监听器
            WebSocketListener listener = new WebSocketListener() {
                private final StringBuilder partialResult = new StringBuilder();

                @Override
                public void onOpen(WebSocket webSocket, Response response) {
                    logger.info("WebSocket连接已打开 - TaskId: {}", taskId);

                    try {
                        // 发送开始指令
                        Map<String, Object> startParams = new HashMap<>();
                        startParams.put("header", createHeader("StartRecognition", taskId));
                        startParams.put("payload", createPayload());

                        String startMessage = objectMapper.writeValueAsString(startParams);
                        webSocket.send(startMessage);
                        logger.debug("发送开始识别指令 - TaskId: {}", taskId);
                    } catch (Exception e) {
                        logger.error("发送开始指令失败 - TaskId: {}", taskId, e);
                        resultSink.tryEmitError(e);
                        webSocket.close(1000, "发送开始指令失败");
                    }
                }

                @Override
                public void onMessage(WebSocket webSocket, String text) {
                    try {
                        JsonNode response = objectMapper.readTree(text);
                        String header = response.path("header").path("name").asText();

                        if ("RecognitionStarted".equals(header)) {
                            logger.debug("识别已开始 - TaskId: {}", taskId);
                        } else if ("RecognitionResultChanged".equals(header)) {
                            // 中间结果
                            String result = response.path("payload").path("result").asText();
                            logger.debug("识别中间结果 - TaskId: {}, 结果: {}", taskId, result);
                            resultSink.tryEmitNext(result);
                            partialResult.setLength(0);
                            partialResult.append(result);
                        } else if ("RecognitionCompleted".equals(header)) {
                            // 最终结果
                            String result = response.path("payload").path("result").asText();
                            logger.info("识别完成 - TaskId: {}, 最终结果: {}", taskId, result);

                            // 如果最终结果与上一个中间结果不同，则发送最终结果
                            if (!result.equals(partialResult.toString())) {
                                resultSink.tryEmitNext(result);
                            }

                            // 发送结束指令
                            Map<String, Object> stopParams = new HashMap<>();
                            stopParams.put("header", createHeader("StopRecognition", taskId));
                            String stopMessage = objectMapper.writeValueAsString(stopParams);
                            webSocket.send(stopMessage);
                        } else if ("TaskFailed".equals(header)) {
                            // 任务失败
                            String errorCode = response.path("header").path("status").asInt() + "";
                            String errorMessage = response.path("header").path("message").asText("未知错误");
                            logger.error("识别任务失败 - TaskId: {}, 错误码: {}, 错误信息: {}", taskId, errorCode, errorMessage);
                            resultSink.tryEmitError(new RuntimeException("识别失败: " + errorCode + " - " + errorMessage));
                        }
                    } catch (Exception e) {
                        logger.error("处理WebSocket消息失败 - TaskId: {}", taskId, e);
                        resultSink.tryEmitError(e);
                    }
                }

                @Override
                public void onMessage(WebSocket webSocket, ByteString bytes) {
                    logger.debug("收到二进制消息 - TaskId: {}, 大小: {} 字节", taskId, bytes.size());
                }

                @Override
                public void onClosing(WebSocket webSocket, int code, String reason) {
                    logger.info("WebSocket正在关闭 - TaskId: {}, 代码: {}, 原因: {}", taskId, code, reason);
                    webSocket.close(1000, "客户端主动关闭");
                    activeWebSockets.remove(taskId);
                }

                @Override
                public void onClosed(WebSocket webSocket, int code, String reason) {
                    logger.info("WebSocket已关闭 - TaskId: {}, 代码: {}, 原因: {}", taskId, code, reason);
                    resultSink.tryEmitComplete();
                    activeWebSockets.remove(taskId);
                }

                @Override
                public void onFailure(WebSocket webSocket, Throwable t, Response response) {
                    logger.error("WebSocket连接失败 - TaskId: {}", taskId, t);
                    resultSink.tryEmitError(t);
                    activeWebSockets.remove(taskId);
                }
            };

            // 创建WebSocket连接
            Request request = new Request.Builder()
                    .url(url)
                    .addHeader("X-NLS-Token", nlsToken)
                    .build();

            WebSocket webSocket = client.newWebSocket(request, listener);
            activeWebSockets.put(taskId, webSocket);

            // 标记是否已经发送了停止信号
            AtomicBoolean stopSent = new AtomicBoolean(false);

            // 订阅音频流并发送数据
            audioStream.subscribe(
                    data -> {
                        try {
                            if (activeWebSockets.containsKey(taskId)) {
                                // 发送二进制音频数据
                                webSocket.send(ByteString.of(data));
                            }
                        } catch (Exception e) {
                            logger.error("发送音频数据时发生错误 - TaskId: {}", taskId, e);
                            resultSink.tryEmitError(e);
                        }
                    },
                    error -> {
                        logger.error("音频流错误 - TaskId: {}", taskId, error);
                        resultSink.tryEmitError(error);
                        if (activeWebSockets.containsKey(taskId)) {
                            try {
                                // 发送结束指令
                                if (!stopSent.getAndSet(true)) {
                                    Map<String, Object> stopParams = new HashMap<>();
                                    stopParams.put("header", createHeader("StopRecognition", taskId));
                                    String stopMessage = objectMapper.writeValueAsString(stopParams);
                                    webSocket.send(stopMessage);
                                }
                                webSocket.close(1000, "音频流错误");
                            } catch (Exception e) {
                                logger.error("关闭WebSocket时发生错误 - TaskId: {}", taskId, e);
                            } finally {
                                activeWebSockets.remove(taskId);
                            }
                        }
                    },
                    () -> {
                        if (activeWebSockets.containsKey(taskId) && !stopSent.getAndSet(true)) {
                            try {
                                // 发送结束指令
                                Map<String, Object> stopParams = new HashMap<>();
                                stopParams.put("header", createHeader("StopRecognition", taskId));
                                String stopMessage = objectMapper.writeValueAsString(stopParams);
                                webSocket.send(stopMessage);

                                // 等待一段时间后关闭连接，确保结果能够返回
                                Thread.sleep(1000);
                                webSocket.close(1000, "音频流结束");
                            } catch (Exception e) {
                                logger.error("关闭WebSocket时发生错误 - TaskId: {}", taskId, e);
                                resultSink.tryEmitError(e);
                            }
                        }
                    });

        } catch (Exception e) {
            logger.error("创建语音识别会话时发生错误", e);
            return Flux.error(e);
        }

        return resultSink.asFlux();
    }

    /**
     * 创建请求头部
     */
    private Map<String, Object> createHeader(String name, String taskId) {
        Map<String, Object> header = new HashMap<>();
        header.put("message_id", UUID.randomUUID().toString());
        header.put("task_id", taskId);
        header.put("namespace", "SpeechRecognizer");
        header.put("name", name);
        header.put("appkey", appKey);
        return header;
    }

    /**
     * 创建请求负载
     */
    private Map<String, Object> createPayload() {
        Map<String, Object> payload = new HashMap<>();
        payload.put("format", FORMAT);
        payload.put("sample_rate", SAMPLE_RATE);
        payload.put("enable_punctuation_prediction", true);
        payload.put("enable_inverse_text_normalization", true);
        return payload;
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

    // 在服务关闭时释放资源
    public void shutdown() {
        // 关闭所有活跃的WebSocket连接
        activeWebSockets.forEach((id, webSocket) -> {
            try {
                webSocket.close(1000, "服务关闭");
            } catch (Exception e) {
                logger.error("关闭WebSocket时发生错误 - TaskId: {}", id, e);
            }
        });
        activeWebSockets.clear();
    }
}