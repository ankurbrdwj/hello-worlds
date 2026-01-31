package com.ankur.url.shortner.service.impl;

import com.ankur.url.shortner.entity.UrlMapping;
import com.ankur.url.shortner.exception.UrlExpiredException;
import com.ankur.url.shortner.exception.UrlNotFoundException;
import com.ankur.url.shortner.repository.UrlRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("UrlResolverService Tests")
class UrlResolverServiceTest {

    @Mock
    private UrlRepository repository;

    @InjectMocks
    private UrlResolverService service;

    private UrlMapping validUrlMapping;

    @BeforeEach
    void setUp() {
        validUrlMapping = UrlMapping.builder()
                .originalUrl("https://www.example.com")
                .shortCode("abc123")
                .createdAt(Instant.now())
                .expiresAt(null)
                .build();
    }

    @Test
    @DisplayName("Should resolve URL successfully when short code exists and not expired")
    void shouldResolveUrlSuccessfully() {
        // Given
        String shortCode = "abc123";
        when(repository.findByShortCode(shortCode)).thenReturn(Optional.of(validUrlMapping));

        // When
        String result = service.resolveUrl(shortCode);

        // Then
        assertThat(result).isEqualTo("https://www.example.com");
        verify(repository).findByShortCode(shortCode);
    }

    @Test
    @DisplayName("Should throw UrlNotFoundException when short code does not exist")
    void shouldThrowUrlNotFoundExceptionWhenShortCodeDoesNotExist() {
        // Given
        String nonExistentCode = "notfound";
        when(repository.findByShortCode(nonExistentCode)).thenReturn(Optional.empty());

        // When/Then
        assertThatThrownBy(() -> service.resolveUrl(nonExistentCode))
                .isInstanceOf(UrlNotFoundException.class)
                .hasMessage("Short URL not found: notfound");

        verify(repository).findByShortCode(nonExistentCode);
    }

    @Test
    @DisplayName("Should throw UrlExpiredException when URL has expired")
    void shouldThrowUrlExpiredExceptionWhenUrlHasExpired() {
        // Given
        String shortCode = "expired123";
        Instant pastDate = Instant.now().minus(7, ChronoUnit.DAYS);
        UrlMapping expiredMapping = UrlMapping.builder()
                .originalUrl("https://www.example.com")
                .shortCode(shortCode)
                .createdAt(Instant.now().minus(14, ChronoUnit.DAYS))
                .expiresAt(pastDate)
                .build();

        when(repository.findByShortCode(shortCode)).thenReturn(Optional.of(expiredMapping));

        // When/Then
        assertThatThrownBy(() -> service.resolveUrl(shortCode))
                .isInstanceOf(UrlExpiredException.class)
                .hasMessage("Short URL has expired: expired123");

        verify(repository).findByShortCode(shortCode);
    }

    @Test
    @DisplayName("Should resolve URL successfully when expiration is in the future")
    void shouldResolveUrlWhenExpirationIsInFuture() {
        // Given
        String shortCode = "future123";
        Instant futureDate = Instant.now().plus(7, ChronoUnit.DAYS);
        UrlMapping futureMapping = UrlMapping.builder()
                .originalUrl("https://www.example.com/future")
                .shortCode(shortCode)
                .createdAt(Instant.now())
                .expiresAt(futureDate)
                .build();

        when(repository.findByShortCode(shortCode)).thenReturn(Optional.of(futureMapping));

        // When
        String result = service.resolveUrl(shortCode);

        // Then
        assertThat(result).isEqualTo("https://www.example.com/future");
        verify(repository).findByShortCode(shortCode);
    }

    @Test
    @DisplayName("Should resolve URL successfully when no expiration date is set")
    void shouldResolveUrlWhenNoExpirationDateSet() {
        // Given
        String shortCode = "noexpiry";
        UrlMapping noExpiryMapping = UrlMapping.builder()
                .originalUrl("https://www.example.com/noexpiry")
                .shortCode(shortCode)
                .createdAt(Instant.now())
                .expiresAt(null)
                .build();

        when(repository.findByShortCode(shortCode)).thenReturn(Optional.of(noExpiryMapping));

        // When
        String result = service.resolveUrl(shortCode);

        // Then
        assertThat(result).isEqualTo("https://www.example.com/noexpiry");
        verify(repository).findByShortCode(shortCode);
    }
}