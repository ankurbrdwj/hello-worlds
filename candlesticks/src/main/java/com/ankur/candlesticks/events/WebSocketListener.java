package com.ankur.candlesticks.events;

import com.ankur.candlesticks.config.PartnerConfig;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;
import org.springframework.web.socket.client.WebSocketClient;

@Slf4j
@Service
@RequiredArgsConstructor
public class WebSocketListener {
  private final PartnerConfig partnerConfig;

  private final InstrumentsHandler instrumentsHandler;
  private final QuotesHandler quotesStreamHandler;
  private final WebSocketClient webSocketClient;

  @EventListener(ApplicationReadyEvent.class)
  public void listenWebSockets() {
    if (!partnerConfig.isEnabled()) {
      log.info("Partner connection is not enabled in the application");
      return;
    }

    log.info("Connecting to partner server...");

    //var webSocketClient = new StandardWebSocketClient();
    webSocketClient.execute(instrumentsHandler, partnerConfig.getInstrumentsUri());
    webSocketClient.execute(quotesStreamHandler, partnerConfig.getQuotesUri());
  }

  }
