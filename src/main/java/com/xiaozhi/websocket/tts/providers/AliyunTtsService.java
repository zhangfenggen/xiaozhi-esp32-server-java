package com.xiaozhi.websocket.tts.providers;

import com.alibaba.nls.client.AccessToken;
import com.alibaba.nls.client.protocol.NlsClient;
import com.alibaba.nls.client.protocol.OutputFormatEnum;
import com.alibaba.nls.client.protocol.SampleRateEnum;
import com.alibaba.nls.client.protocol.tts.SpeechSynthesizer;
import com.alibaba.nls.client.protocol.tts.SpeechSynthesizerListener;
import com.alibaba.nls.client.protocol.tts.SpeechSynthesizerResponse;
import com.xiaozhi.entity.SysConfig;
import com.xiaozhi.utils.AudioUtils;
import com.xiaozhi.websocket.tts.TtsService;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.function.Consumer;

public class AliyunTtsService implements TtsService {
    private static final Logger logger = LoggerFactory.getLogger(AliyunTtsService.class);

    private static final String PROVIDER_NAME = "aliyun";

    // 阿里云NLS服务的默认URL
    private static final String NLS_URL = "wss://nls-gateway.aliyuncs.com/ws/v1";

    // 阿里云配置
    private final String accessKeyId;
    private final String accessKeySecret;
    private final String appKey;
    private final String voiceName;
    private final String outputPath;

    // 存储Token信息
    private String token;
    private long expireTime;

    // 全局共享的NLS客户端
    private NlsClient client;

    public AliyunTtsService(SysConfig config,
            String voiceName, String outputPath) {
        this.accessKeyId = config.getAppId();
        this.accessKeySecret = config.getApiSecret();
        this.appKey = config.getApiKey();
        this.voiceName = voiceName;
        this.outputPath = outputPath;

        // 初始化NLS客户端（如果尚未初始化）
        initClient();
    }

    private void initClient() {
        try {
            // 获取有效Token
            String accessToken = getValidToken();
            if (accessToken != null) {
                // 创建NLS客户端实例
                client = new NlsClient(NLS_URL, accessToken);
            }
        } catch (Exception e) {
            logger.error("初始化阿里云NLS客户端失败", e);
        }
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
        // 保留原有的一句话转语音功能
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        CountDownLatch latch = new CountDownLatch(1);

        try {
            // 创建语音合成请求
            SpeechSynthesizer synthesizer = new SpeechSynthesizer(client, getSynthesizerListener());

            // 设置appKey
            synthesizer.setAppKey(appKey);
            // 设置语音输出格式
            synthesizer.setFormat(OutputFormatEnum.MP3);
            // 设置采样率
            synthesizer.setSampleRate(SampleRateEnum.SAMPLE_RATE_16K);
            // 设置语音
            synthesizer.setVoice(voiceName);
            // 设置音量
            synthesizer.setVolume(50);
            // 设置语速
            synthesizer.setSpeechRate(0);
            // 设置语调
            synthesizer.setPitchRate(0);

            // 开始语音合成
            synthesizer.start();
            // 发送文本
            synthesizer.setText(text);
            // 结束语音合成
            synthesizer.waitForComplete();

            // 等待语音合成完成
            latch.await();

            // 保存音频文件
            String audioFileName = getAudioFileName();
            String filePath = outputPath + audioFileName;

            try (FileOutputStream fileOutputStream = new FileOutputStream(filePath)) {
                fileOutputStream.write(outputStream.toByteArray());
            }

            return filePath;
        } catch (Exception e) {
            logger.error("阿里云语音合成失败: {}", e.getMessage(), e);
            throw e;
        }
    }

    @Override
    public void streamTextToSpeech(String text, Consumer<byte[]> audioDataConsumer) throws Exception {
        try {
            // 创建语音合成请求
            SpeechSynthesizer synthesizer = new SpeechSynthesizer(client, getSynthesizerListener());
            // 设置appKey
            synthesizer.setAppKey(appKey);
            // 设置PCM格式输出，便于直接处理
            synthesizer.setFormat(OutputFormatEnum.PCM);
            // 设置采样率
            synthesizer.setSampleRate(SampleRateEnum.SAMPLE_RATE_16K);
            // 设置语音
            synthesizer.setVoice(voiceName);
            // 设置音量
            synthesizer.setVolume(50);
            // 设置语速
            synthesizer.setSpeechRate(0);
            // 设置语调
            synthesizer.setPitchRate(0);

            // 开始语音合成
            synthesizer.start();
            // 发送文本
            synthesizer.setText(text);
            // 结束语音合成
            synthesizer.waitForComplete();

        } catch (Exception e) {
            logger.error("阿里云流式语音合成失败: {}", e.getMessage(), e);
            throw e;
        }
    }

    private static SpeechSynthesizerListener getSynthesizerListener() {
        SpeechSynthesizerListener listener = null;
        try {
            listener = new SpeechSynthesizerListener() {
                File f = new File("tts_test.wav");
                FileOutputStream fout = new FileOutputStream(f);
                private boolean firstRecvBinary = true;

                // 语音合成结束
                @Override
                public void onComplete(SpeechSynthesizerResponse response) {
                    // 调用onComplete时表示所有TTS数据已接收完成，因此为整个合成数据的延迟。该延迟可能较大，不一定满足实时场景。
                    System.out.println("name: " + response.getName() +
                            ", status: " + response.getStatus() +
                            ", output file :" + f.getAbsolutePath());
                }

                // 语音合成的语音二进制数据
                @Override
                public void onMessage(ByteBuffer message) {
                    try {
                        if (firstRecvBinary) {
                            // 计算首包语音流的延迟，收到第一包语音流时，即可以进行语音播放，以提升响应速度（特别是实时交互场景下）。
                            firstRecvBinary = false;
                            long now = System.currentTimeMillis();
                        }
                        byte[] bytesArray = new byte[message.remaining()];
                        message.get(bytesArray, 0, bytesArray.length);
                        fout.write(bytesArray);
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }

                @Override
                public void onFail(SpeechSynthesizerResponse response) {
                    // task_id是调用方和服务端通信的唯一标识，当遇到问题时需要提供task_id以便排查。
                    System.out.println(
                            "task_id: " + response.getTaskId() +
                    // 状态码 20000000 表示识别成功
                                    ", status: " + response.getStatus() +
                    // 错误信息
                                    ", status_text: " + response.getStatusText());
                }
            };
        } catch (Exception e) {
            e.printStackTrace();
        }
        return listener;
    }

    /**
     * 获取有效的阿里云NLS Token
     */
    private String getValidToken() {
        try {
            // 检查当前token是否存在且未过期
            long currentTime = System.currentTimeMillis() / 1000; // 转换为秒
            if (token != null && expireTime > currentTime) {
                return token;
            }

            // 获取新token
            AccessToken accessToken = new AccessToken(accessKeyId, accessKeySecret);
            accessToken.apply();
            token = accessToken.getToken();
            expireTime = accessToken.getExpireTime();

            logger.info("成功获取阿里云NLS Token，过期时间: {}", expireTime);
            return token;
        } catch (Exception e) {
            logger.error("获取阿里云NLS Token时发生错误", e);
            return null;
        }
    }

    // 关闭资源
    public void shutdown() {
        if (client != null) {
            client.shutdown();
            client = null;
            logger.info("NLS客户端已关闭");
        }
    }
}
