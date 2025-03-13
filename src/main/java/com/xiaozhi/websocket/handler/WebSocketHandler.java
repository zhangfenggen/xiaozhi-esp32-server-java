package com.xiaozhi.websocket.handler;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.xiaozhi.entity.SysDevice;

import com.xiaozhi.service.SysDeviceService;
import com.xiaozhi.websocket.service.TextToSpeechService;
import com.xiaozhi.websocket.service.AudioService;
import com.xiaozhi.websocket.service.MessageService;
import com.xiaozhi.websocket.service.SpeechToTextService;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.util.StringUtils;
import org.springframework.web.socket.BinaryMessage;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class WebSocketHandler extends TextWebSocketHandler {

    @Autowired
    private SysDeviceService deviceService;

    @Autowired
    private AudioService audioService;

    @Autowired
    @Qualifier("WebSocketMessageService")
    private MessageService messageService;

    @Autowired
    private TextToSpeechService textToSpeechService;

    @Autowired
    private SpeechToTextService speechToTextService;

    private static final Logger logger = LoggerFactory.getLogger(WebSocketHandler.class);

    // 用于存储所有连接的会话
    private static final ConcurrentHashMap<String, WebSocketSession> SESSIONS = new ConcurrentHashMap<>();

    // 用于存储设备ID和会话的映射关系
    private static final ConcurrentHashMap<String, WebSocketSession> DEVICE_SESSIONS = new ConcurrentHashMap<>();

    // 用于存储会话和设备ID的映射关系
    private static final ConcurrentHashMap<String, String> SESSION_DEVICES = new ConcurrentHashMap<>();

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws Exception {
        String sessionId = session.getId();
        SESSIONS.put(sessionId, session);

        Map<String, List<String>> headers = session.getHandshakeHeaders();

        // 从请求头中获取设备ID
        String deviceId = headers.get("device-id").get(0);
        if (deviceId != null) {
            DEVICE_SESSIONS.put(deviceId, session);
            SESSION_DEVICES.put(sessionId, deviceId);
            logger.info("WebSocket连接建立成功 - SessionId: {}, DeviceId: {}", sessionId, deviceId);
        } else {
            logger.info("WebSocket连接建立成功 - SessionId: {} (无设备ID)", sessionId);
        }
    }

    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) throws Exception {
        String payload = message.getPayload();
        logger.info("收到文本消息 - SessionId: {}, Message: {}", session.getId(), payload);

        // 更新设备在线时间
        String deviceId = SESSION_DEVICES.get(session.getId());
        if (deviceId != null) {
            SysDevice device = new SysDevice();
            device.setDeviceId(deviceId);
            device.setLastLogin(new Date());
            deviceService.update(device);
        }

        // 解析JSON消息
        try {
            JsonNode jsonNode = objectMapper.readTree(payload);
            String messageType = jsonNode.path("type").asText();

            switch (messageType) {
                case "hello":
                    handleHelloMessage(session, jsonNode);
                    break;
                case "listen":
                    handleListenMessage(session, jsonNode);
                    break;
                case "abort":
                    handleAbortMessage(session, jsonNode);
                    break;
                case "iot":
                    handleIotMessage(session, jsonNode);
                    break;
                default:
                    logger.warn("未知的消息类型: {}", messageType);
                    break;
            }
        } catch (Exception e) {
            logger.error("处理消息失败", e);
        }
    }

    // 处理客户端发送的音频
    @Override
    protected void handleBinaryMessage(WebSocketSession session, BinaryMessage message) {
        String sessionId = session.getId();
        byte[] opusData = message.getPayload().array();
        try {

            // 将所有音频处理逻辑委托给AudioService
            byte[] completeAudio = audioService.processIncomingAudio(sessionId, opusData);

            if (completeAudio != null) {
                logger.info("检测到语音结束 - SessionId: {}, 音频大小: {} 字节", sessionId, completeAudio.length);
                // 调用 SpeechToTextService 进行语音识别
                String jsonResult = speechToTextService.processAudio(completeAudio);
                JsonNode resultNode = objectMapper.readTree(jsonResult);
                String result = resultNode.path("text").asText("");
                if (StringUtils.hasText(result)) {
                    logger.info("语音识别结果 - SessionId: {}, 内容: {}", sessionId, result);
                    messageService.sendMessage(session, "stt", "stop", result);
                    // 将识别到的用户的对话发送给模型进行推测

                }
            }
        } catch (Exception e) {
            logger.error("处理二进制消息失败", e);
        }

    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) throws Exception {
        String sessionId = session.getId();
        String deviceId = SESSION_DEVICES.remove(sessionId);

        if (deviceId != null) {
            // 更新设备在线时间
            if (deviceId != null) {
                SysDevice device = new SysDevice();
                device.setDeviceId(deviceId);
                device.setLastLogin(new Date());
                deviceService.update(device);
            }
            DEVICE_SESSIONS.remove(deviceId);
            logger.info("WebSocket连接关闭 - SessionId: {}, DeviceId: {}, Status: {}", sessionId, deviceId, status);
        } else {
            logger.info("WebSocket连接关闭 - SessionId: {}, Status: {}", sessionId, status);
        }

        SESSIONS.remove(sessionId);
    }

    @Override
    public void handleTransportError(WebSocketSession session, Throwable exception) throws Exception {
        logger.error("WebSocket传输错误 - SessionId: {}", session.getId(), exception);
    }

    // 处理客户端发送的hello消息
    private void handleHelloMessage(WebSocketSession session, JsonNode jsonNode) throws IOException {
        logger.info("收到hello消息 - SessionId: {}", session.getId());

        // 验证客户端hello消息
        if (!jsonNode.path("transport").asText().equals("websocket")) {
            logger.warn("不支持的传输方式: {}", jsonNode.path("transport").asText());
            session.close(CloseStatus.NOT_ACCEPTABLE);
            return;
        }

        // 解析音频参数
        JsonNode audioParams = jsonNode.path("audio_params");
        String format = audioParams.path("format").asText();
        int sampleRate = audioParams.path("sample_rate").asInt();
        int channels = audioParams.path("channels").asInt();
        int frameDuration = audioParams.path("frame_duration").asInt();

        logger.info("客户端音频参数 - 格式: {}, 采样率: {}, 声道: {}, 帧时长: {}ms",
                format, sampleRate, channels, frameDuration);

        // 回复hello消息
        ObjectNode response = objectMapper.createObjectNode();
        response.put("type", "hello");
        response.put("transport", "websocket");

        // 添加音频参数（可以根据服务器配置调整）
        ObjectNode responseAudioParams = response.putObject("audio_params");
        responseAudioParams.put("format", format);
        responseAudioParams.put("sample_rate", sampleRate);
        responseAudioParams.put("channels", channels);
        responseAudioParams.put("frame_duration", frameDuration);

        session.sendMessage(new TextMessage(response.toString()));
    }

    // 处理客户端发送的listen消息
    private void handleListenMessage(WebSocketSession session, JsonNode jsonNode) throws Exception {
        String sessionId = session.getId();
        String state = jsonNode.path("state").asText();
        String mode = jsonNode.path("mode").asText();

        logger.info("收到listen消息 - SessionId: {}, State: {}, Mode: {}", sessionId, state, mode);

        SysDevice device = new SysDevice();
        device.setDeviceId(SESSION_DEVICES.get(sessionId));
        device.setSessionId(sessionId);

        List<SysDevice> devices = deviceService.query(device);

        if (devices.isEmpty()) {
            SysDevice codeResult = deviceService.generateCode(device);
            String audioFilePath;
            if (StringUtils.isEmpty(codeResult.getAudioPath())) {
                audioFilePath = textToSpeechService.textToSpeech("请到设备管理页面添加设备，输入验证码" + codeResult.getCode());
                codeResult.setDeviceId(SESSION_DEVICES.get(sessionId));
                codeResult.setSessionId(sessionId);
                codeResult.setAudioPath(audioFilePath);
                deviceService.updateCode(codeResult);
            } else {
                audioFilePath = codeResult.getAudioPath();
            }
            logger.info("设备未绑定，返回验证码");
            audioService.sendAudio(session, audioFilePath, codeResult.getCode());
            return;
        }

        // 根据state处理不同的监听状态
        switch (state) {
            case "start":
                // 开始监听，准备接收音频数据
                logger.info("开始监听 - Mode: {}", mode);
                break;
            case "stop":
                // 停止监听
                logger.info("停止监听");
                break;
            case "detect":
                // 检测到唤醒词
                String text = jsonNode.path("text").asText();
                logger.info("检测到唤醒词: {}", text);
                // 可以触发进一步的处理逻辑
                break;
            default:
                logger.warn("未知的listen状态: {}", state);
                break;
        }
    }

    // 处理客户端发送的abort消息
    private void handleAbortMessage(WebSocketSession session, JsonNode jsonNode) {
        String sessionId = session.getId();
        String reason = jsonNode.path("reason").asText();

        logger.info("收到abort消息 - SessionId: {}, Reason: {}", sessionId, reason);

        // 根据reason处理中断逻辑
        // 例如，如果是wake_word_detected，可能需要停止当前TTS播放
    }

    // 处理客户端发送的IoT消息
    private void handleIotMessage(WebSocketSession session, JsonNode jsonNode) {
        String sessionId = session.getId();
        logger.info("收到IoT消息 - SessionId: {}", sessionId);

        // 处理设备描述信息
        if (jsonNode.has("descriptors")) {
            JsonNode descriptors = jsonNode.path("descriptors");
            logger.info("收到设备描述信息: {}", descriptors);
            // 处理设备描述信息的逻辑
        }

        // 处理设备状态更新
        if (jsonNode.has("states")) {
            JsonNode states = jsonNode.path("states");
            logger.info("收到设备状态更新: {}", states);
            // 处理设备状态更新的逻辑
        }
    }

    // 广播消息给所有连接的客户端
    public void broadcastMessage(String message) {
        SESSIONS.forEach((sessionId, session) -> {
            try {
                if (session.isOpen()) {
                    session.sendMessage(new TextMessage(message));
                }
            } catch (IOException e) {
                logger.error("广播消息失败 - SessionId: {}", sessionId, e);
            }
        });
    }

    // 获取当前连接数
    public int getConnectionCount() {
        return SESSIONS.size();
    }

    // 获取设备连接数
    public int getDeviceConnectionCount() {
        return DEVICE_SESSIONS.size();
    }

    // 判断指定设备是否连接
    public boolean isDeviceConnected(String deviceId) {
        WebSocketSession session = DEVICE_SESSIONS.get(deviceId);
        return session != null && session.isOpen();
    }

    // 关闭指定设备的会话
    public void closeDeviceSession(String deviceId) {
        WebSocketSession session = DEVICE_SESSIONS.get(deviceId);
        if (session != null && session.isOpen()) {
            try {
                session.close();
            } catch (IOException e) {
                logger.error("关闭设备会话失败 - DeviceId: {}", deviceId, e);
            }
        }
    }

}
