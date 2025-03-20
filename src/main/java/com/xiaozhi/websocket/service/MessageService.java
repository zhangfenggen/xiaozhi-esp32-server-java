package com.xiaozhi.websocket.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.xiaozhi.websocket.handler.WebSocketHandler;

import java.io.IOException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;

@Service("WebSocketMessageService")
public class MessageService {

    private static final Logger logger = LoggerFactory.getLogger(WebSocketHandler.class);

    // 发送文本消息给指定会话
    public void sendMessage(WebSocketSession session, String type, String state) {
        try {
            if (session != null && session.isOpen()) {
                ObjectNode response = new ObjectMapper().createObjectNode();
                response.put("session_id", session.getId());
                response.put("type", type);
                response.put("state", state);
                session.sendMessage(new TextMessage(response.toString()));
                logger.info("发送消息 - SessionId: {}, Message: {}", session.getId(), response.toString());
            } else {
                logger.warn("无法发送消息 - 会话已关闭或为null");
            }
        } catch (IOException e) {
            logger.error("发送消息时发生异常 - SessionId: {}, Message: {}", session.getId(), e.getMessage());
        }
    }

    // 发送文本消息给指定会话
    public void sendMessage(WebSocketSession session, String type, String state, String message) {
        try {
            if (session != null && session.isOpen()) {
                ObjectNode response = new ObjectMapper().createObjectNode();
                response.put("session_id", session.getId());
                response.put("type", type);
                response.put("state", state);
                response.put("text", message);
                session.sendMessage(new TextMessage(response.toString()));
                logger.info("发送消息 - SessionId: {}, Message: {}", session.getId(), response.toString());
            } else {
                logger.warn("无法发送消息 - 会话已关闭或为null");
            }
        } catch (IOException e) {
            logger.error("发送消息时发生异常 - SessionId: {}, Message: {}", session.getId(), e.getMessage());
        }
    }

}
