package com.xiaozhi.websocket.config;

import com.xiaozhi.websocket.handler.ReactiveWebSocketHandler;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.HandlerMapping;
import org.springframework.web.reactive.handler.SimpleUrlHandlerMapping;
import org.springframework.web.reactive.socket.WebSocketHandler;
import org.springframework.web.reactive.socket.server.support.WebSocketHandlerAdapter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.util.HashMap;
import java.util.Map;

@Configuration
public class WebFluxWebSocketConfig {

    private static final Logger logger = LoggerFactory.getLogger(WebFluxWebSocketConfig.class);

    @Bean
    public HandlerMapping webSocketHandlerMapping(ReactiveWebSocketHandler webSocketHandler) {
        Map<String, WebSocketHandler> map = new HashMap<>();
        map.put("/ws/xiaozhi/v1/", webSocketHandler);

        SimpleUrlHandlerMapping handlerMapping = new SimpleUrlHandlerMapping();
        handlerMapping.setOrder(1);
        handlerMapping.setUrlMap(map);

        logger.info("==========================================================");
        logger.info("WebFlux WebSocket配置已加载，路径: /ws/xiaozhi/v1/");
        logger.info("==========================================================");
        
        return handlerMapping;
    }

    @Bean
    public WebSocketHandlerAdapter handlerAdapter() {
        logger.info("WebSocketHandlerAdapter 已加载");
        return new WebSocketHandlerAdapter();
    }
}