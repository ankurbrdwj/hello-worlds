package com.ankur.candlesticks.events;

import com.ankur.candlesticks.config.PartnerConfig;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.web.socket.client.standard.StandardWebSocketClient;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@DataJpaTest
public class WebSocketListenerTest {

  @Mock
  private PartnerConfig partnerConfig;

  @Mock
  private InstrumentsHandler instrumentsHandler;

  @Mock
  private QuotesHandler quotesStreamHandler;

  @Mock
  private StandardWebSocketClient webSocketClient;

  private WebSocketListener webSocketListener;

  @BeforeEach
  void setUp() {
    MockitoAnnotations.openMocks(this);
    // Mock PartnerConfig to simulate that partner connection is enabled
    // Mock PartnerConfig and set properties
    partnerConfig = Mockito.mock(PartnerConfig.class);
    Mockito.when(partnerConfig.isEnabled()).thenReturn(true);
    Mockito.when(partnerConfig.getInstrumentsUri()).thenReturn("ws://localhost:8032/instruments");
    Mockito.when(partnerConfig.getQuotesUri()).thenReturn("ws://localhost:8032/quotes");
    webSocketListener = new WebSocketListener(partnerConfig, instrumentsHandler
      , quotesStreamHandler,webSocketClient);


  }

  @Test
  public void testWebSocketListenerWithEnabledPartnerConfig() {
    when(webSocketClient.execute(any(), anyString())).thenReturn(null); // Mock execution

    // Execute WebSocket listener method
    webSocketListener.listenWebSockets();

    // Verify that the webSocketClient executed the handlers
    verify(webSocketClient, times(1)).execute(instrumentsHandler, partnerConfig.getInstrumentsUri());
    verify(webSocketClient, times(1)).execute(quotesStreamHandler, partnerConfig.getQuotesUri());
  }

  @Test
  public void testWebSocketListenerWithDisabledPartnerConfig() {
    // Mock PartnerConfig to simulate that partner connection is disabled
    when(partnerConfig.isEnabled()).thenReturn(false);

    // Execute WebSocket listener method
    webSocketListener.listenWebSockets();

    // Verify that no WebSocket connection was attempted
    verifyNoInteractions(instrumentsHandler);
    verifyNoInteractions(quotesStreamHandler);
  }
}
