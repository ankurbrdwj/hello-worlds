package com.ankur.exchange.feed.config;

import com.ankur.exchange.feed.websocket.InstrumentWebSocketHandler;
import com.ankur.exchange.feed.websocket.QuotesWebSocketHandler;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;

@Configuration
@EnableWebSocket
@RequiredArgsConstructor
public class WebSocketConfig implements WebSocketConfigurer {

    private final InstrumentWebSocketHandler instrumentWebSocketHandler;
    private final QuotesWebSocketHandler quotesWebSocketHandler;

    @Override
    public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
        registry.addHandler(instrumentWebSocketHandler, "/instruments")
                .setAllowedOrigins("*");

        registry.addHandler(quotesWebSocketHandler, "/quotes")
                .setAllowedOrigins("*");
    }
}