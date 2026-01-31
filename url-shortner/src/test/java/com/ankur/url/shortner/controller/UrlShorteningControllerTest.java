package com.ankur.url.shortner.controller;

import com.ankur.url.shortner.dto.CreateShortUrlRequest;
import com.ankur.url.shortner.entity.UrlMapping;
import com.ankur.url.shortner.exception.UrlExpiredException;
import com.ankur.url.shortner.exception.UrlNotFoundException;
import com.ankur.url.shortner.service.UrlShortenerService;
import com.ankur.url.shortner.service.impl.UrlResolverService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Optional;

import static org.hamcrest.Matchers.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(UrlShorteningController.class)
@DisplayName("UrlShorteningController Integration Tests")
class UrlShorteningControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private UrlShortenerService shortenerService;

    @MockBean
    private UrlResolverService resolverService;

    @Test
    @DisplayName("POST /shorten - Should create short URL successfully")
    void shouldCreateShortUrlSuccessfully() throws Exception {
        // Given
        CreateShortUrlRequest request = new CreateShortUrlRequest(
                "https://www.example.com",
                Optional.empty(),
                Optional.empty()
        );

        UrlMapping mockMapping = UrlMapping.builder()
                .originalUrl("https://www.example.com")
                .shortCode("abc123")
                .customAlias(null)
                .createdAt(Instant.now())
                .expiresAt(null)
                .build();

        when(shortenerService.createShortUrl(any(CreateShortUrlRequest.class)))
                .thenReturn(mockMapping);

        // When/Then
        mockMvc.perform(post("/shorten")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.originalUrl").value("https://www.example.com"))
                .andExpect(jsonPath("$.shortCode").value("abc123"))
                .andExpect(jsonPath("$.customAlias").isEmpty())
                .andExpect(jsonPath("$.expiresAt").isEmpty())
                .andExpect(jsonPath("$.createdAt").exists());

        verify(shortenerService).createShortUrl(any(CreateShortUrlRequest.class));
    }

    @Test
    @DisplayName("POST /shorten - Should create short URL with custom alias")
    void shouldCreateShortUrlWithCustomAlias() throws Exception {
        // Given
        CreateShortUrlRequest request = new CreateShortUrlRequest(
                "https://www.example.com",
                Optional.of("myalias"),
                Optional.empty()
        );

        UrlMapping mockMapping = UrlMapping.builder()
                .originalUrl("https://www.example.com")
                .shortCode("myalias")
                .customAlias("myalias")
                .createdAt(Instant.now())
                .expiresAt(null)
                .build();

        when(shortenerService.createShortUrl(any(CreateShortUrlRequest.class)))
                .thenReturn(mockMapping);

        // When/Then
        mockMvc.perform(post("/shorten")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.originalUrl").value("https://www.example.com"))
                .andExpect(jsonPath("$.shortCode").value("myalias"))
                .andExpect(jsonPath("$.customAlias").value("myalias"));

        verify(shortenerService).createShortUrl(any(CreateShortUrlRequest.class));
    }

    @Test
    @DisplayName("POST /shorten - Should create short URL with expiration date")
    void shouldCreateShortUrlWithExpirationDate() throws Exception {
        // Given
        Instant expiresAt = Instant.now().plus(7, ChronoUnit.DAYS);
        CreateShortUrlRequest request = new CreateShortUrlRequest(
                "https://www.example.com",
                Optional.empty(),
                Optional.of(expiresAt)
        );

        UrlMapping mockMapping = UrlMapping.builder()
                .originalUrl("https://www.example.com")
                .shortCode("abc123")
                .customAlias(null)
                .createdAt(Instant.now())
                .expiresAt(expiresAt)
                .build();

        when(shortenerService.createShortUrl(any(CreateShortUrlRequest.class)))
                .thenReturn(mockMapping);

        // When/Then
        mockMvc.perform(post("/shorten")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.expiresAt").exists())
                .andExpect(jsonPath("$.expiresAt").isNotEmpty());

        verify(shortenerService).createShortUrl(any(CreateShortUrlRequest.class));
    }

    @Test
    @DisplayName("POST /shorten - Should return 400 when URL is invalid")
    void shouldReturn400WhenUrlIsInvalid() throws Exception {
        // Given
        CreateShortUrlRequest request = new CreateShortUrlRequest(
                "invalid-url",
                Optional.empty(),
                Optional.empty()
        );

        when(shortenerService.createShortUrl(any(CreateShortUrlRequest.class)))
                .thenThrow(new IllegalArgumentException("Invalid URL format"));

        // When/Then
        mockMvc.perform(post("/shorten")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());

        verify(shortenerService).createShortUrl(any(CreateShortUrlRequest.class));
    }

    @Test
    @DisplayName("GET /{shortCode} - Should resolve URL successfully")
    void shouldResolveUrlSuccessfully() throws Exception {
        // Given
        String shortCode = "abc123";
        String originalUrl = "https://www.example.com";

        when(resolverService.resolveUrl(shortCode)).thenReturn(originalUrl);

        // When/Then
        mockMvc.perform(get("/{shortCode}", shortCode))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.shortCode").value(shortCode))
                .andExpect(jsonPath("$.originalUrl").value(originalUrl));

        verify(resolverService).resolveUrl(shortCode);
    }

    @Test
    @DisplayName("GET /{shortCode} - Should return 404 when short code not found")
    void shouldReturn404WhenShortCodeNotFound() throws Exception {
        // Given
        String shortCode = "notfound";

        when(resolverService.resolveUrl(shortCode))
                .thenThrow(new UrlNotFoundException("Short URL not found: " + shortCode));

        // When/Then
        mockMvc.perform(get("/{shortCode}", shortCode))
                .andExpect(status().isNotFound());

        verify(resolverService).resolveUrl(shortCode);
    }

    @Test
    @DisplayName("GET /{shortCode} - Should return 410 when URL has expired")
    void shouldReturn410WhenUrlHasExpired() throws Exception {
        // Given
        String shortCode = "expired";

        when(resolverService.resolveUrl(shortCode))
                .thenThrow(new UrlExpiredException("Short URL has expired: " + shortCode));

        // When/Then
        mockMvc.perform(get("/{shortCode}", shortCode))
                .andExpect(status().isGone());

        verify(resolverService).resolveUrl(shortCode);
    }
}