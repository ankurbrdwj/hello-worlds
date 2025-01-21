package com.ankur.candlesticks.events;

import com.ankur.candlesticks.message.InstrumentMessage;
import com.ankur.candlesticks.service.InstrumentService;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.Nonnull;
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
public class InstrumentsHandler extends TextWebSocketHandler {

  private final ObjectMapper objectMapper;
  private final InstrumentService instrumentService;


  @Override
  public void afterConnectionEstablished(@NonNull WebSocketSession session) {
    log.info("Connected to instruments stream");
  }

  @Override
  protected void handleTextMessage(@NonNull WebSocketSession session, @Nonnull TextMessage message) throws Exception {
    var instrumentMessage = objectMapper.readValue(message.getPayload(), InstrumentMessage.class);
    log.debug("InstrumentMessage: {}", instrumentMessage);

    if (instrumentMessage.type() == InstrumentMessage.InstrumentType.ADD) {
      instrumentService.addInstrument(instrumentMessage.data().isin(), instrumentMessage.data().description());
    } else if (instrumentMessage.type() == InstrumentMessage.InstrumentType.DELETE) {
      instrumentService.deleteInstrument(instrumentMessage.data().isin());
    } else {
      log.error("Unknown event type");
    }
  }
}
