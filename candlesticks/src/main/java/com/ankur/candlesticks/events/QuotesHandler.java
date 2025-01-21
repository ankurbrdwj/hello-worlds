package com.ankur.candlesticks.events;

import com.ankur.candlesticks.message.QuoteMessage;
import com.ankur.candlesticks.service.QuoteService;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

@Slf4j
@Component
@RequiredArgsConstructor
public class QuotesHandler extends TextWebSocketHandler {

  private final ObjectMapper objectMapper;
  private final QuoteService quoteService;

  @Override
  public void afterConnectionEstablished(@NonNull WebSocketSession session) {
    log.info("Connected to quotes stream");
  }

  @Override
  protected void handleTextMessage(@NonNull WebSocketSession session, TextMessage message) throws Exception {
    var quoteMessage = objectMapper.readValue(message.getPayload(), QuoteMessage.class);
    log.debug("QuoteMessage: {}", quoteMessage);

    quoteService.saveQuote(quoteMessage.data().isin(), quoteMessage.data().price());
  }
}
