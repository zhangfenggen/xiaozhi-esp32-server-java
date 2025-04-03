package com.xiaozhi.websocket.handler;

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.handler.codec.http.FullHttpRequest;
import io.netty.util.AttributeKey;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * WebSocket 握手处理器
 * 负责提取 HTTP 请求中的设备信息，并设置到 Channel 属性中
 */
public class WebSocketHandshakeHandler extends ChannelInboundHandlerAdapter {

    private static final Logger logger = LoggerFactory.getLogger(WebSocketHandshakeHandler.class);

    public static final AttributeKey<String> SESSION_ID = AttributeKey.valueOf("sessionId");
    public static final AttributeKey<String> DEVICE_ID = AttributeKey.valueOf("deviceId");

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
        if (msg instanceof FullHttpRequest) {
            FullHttpRequest req = (FullHttpRequest) msg;

            // 提取设备ID
            String deviceId = req.headers().get("device-id");
            if (deviceId == null || deviceId.isEmpty()) {
                logger.warn("WebSocket连接缺少device-id头");
                ctx.close();
                return;
            }

            // 生成会话ID并存储
            String sessionId = ctx.channel().id().asShortText();
            ctx.channel().attr(SESSION_ID).set(sessionId);
            ctx.channel().attr(DEVICE_ID).set(deviceId);

            logger.info("WebSocket握手请求 - SessionId: {}, DeviceId: {}", sessionId, deviceId);
        }

        // 继续处理请求
        ctx.fireChannelRead(msg);
    }
}
