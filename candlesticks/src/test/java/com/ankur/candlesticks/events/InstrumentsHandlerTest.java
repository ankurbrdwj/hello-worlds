package com.ankur.candlesticks.events;

import com.ankur.candlesticks.message.InstrumentMessage;
import com.ankur.candlesticks.service.InstrumentService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.slf4j.Logger;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import static org.mockito.Mockito.*;

public class InstrumentsHandlerTest {

  @Mock
  private ObjectMapper objectMapper;

  @Mock
  private InstrumentService instrumentService;

  @Mock
  private WebSocketSession webSocketSession;

  @Mock
  private Logger log;

  @InjectMocks
  private InstrumentsHandler instrumentsHandler;

  @BeforeEach
  void setUp() {
    MockitoAnnotations.openMocks(this);
  }

  @Test
  public void testAfterConnectionEstablished() throws Exception {
    // Call the method
    instrumentsHandler.afterConnectionEstablished(webSocketSession);

    // Verify the log message is printed
    verify(log).info("Connected to instruments stream");
  }

  @Test
  public void testHandleTextMessage_AddInstrument() throws Exception {
    // Arrange
    InstrumentMessage instrumentMessage = new InstrumentMessage(
      new InstrumentMessage.InstrumentData("ISIN123", "Test Instrument"),
      InstrumentMessage.InstrumentType.ADD );

    // Mock ObjectMapper behavior
    when(objectMapper.readValue(anyString(), eq(InstrumentMessage.class)))
      .thenReturn(instrumentMessage);

    // Act
    instrumentsHandler.handleTextMessage(webSocketSession, new TextMessage("some payload"));

    // Assert
    verify(instrumentService, times(1)).addInstrument("ISIN123", "Test Instrument");
  }

  @Test
  public void testHandleTextMessage_DeleteInstrument() throws Exception {
    // Arrange
    InstrumentMessage instrumentMessage = new InstrumentMessage(
      new InstrumentMessage.InstrumentData("ISIN123", "Test Instrument"),
      InstrumentMessage.InstrumentType.DELETE );

    // Mock ObjectMapper behavior
    when(objectMapper.readValue(anyString(), eq(InstrumentMessage.class)))
      .thenReturn(instrumentMessage);

    // Act
    instrumentsHandler.handleTextMessage(webSocketSession, new TextMessage("some payload"));

    // Assert
    verify(instrumentService, times(1)).deleteInstrument("ISIN123");
  }
}
