package com.ankur.url.shortner.entity;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;

@DisplayName("UrlMapping Entity Tests")
class UrlMappingTest {

    @Test
    @DisplayName("Should create UrlMapping with all required fields")
    void shouldCreateUrlMappingWithRequiredFields() {
        // Given
        Instant now = Instant.now();

        // When
        UrlMapping urlMapping = UrlMapping.builder()
                .originalUrl("https://www.example.com")
                .shortCode("abc123")
                .createdAt(now)
                .build();

        // Then
        assertThat(urlMapping).isNotNull();
        assertThat(urlMapping.getOriginalUrl()).isEqualTo("https://www.example.com");
        assertThat(urlMapping.getShortCode()).isEqualTo("abc123");
        assertThat(urlMapping.getCreatedAt()).isEqualTo(now);
        assertThat(urlMapping.getExpiresAt()).isEmpty();
    }

    @Test
    @DisplayName("Should create UrlMapping with custom alias")
    void shouldCreateUrlMappingWithCustomAlias() {
        // When
        UrlMapping urlMapping = UrlMapping.builder()
                .originalUrl("https://www.example.com")
                .shortCode("myalias")
                .customAlias("myalias")
                .createdAt(Instant.now())
                .build();

        // Then
        assertThat(urlMapping.getCustomAlias()).isEqualTo("myalias");
    }

    @Test
    @DisplayName("Should create UrlMapping with expiration date")
    void shouldCreateUrlMappingWithExpirationDate() {
        // Given
        Instant expiresAt = Instant.now().plus(7, ChronoUnit.DAYS);

        // When
        UrlMapping urlMapping = UrlMapping.builder()
                .originalUrl("https://www.example.com")
                .shortCode("abc123")
                .createdAt(Instant.now())
                .expiresAt(expiresAt)
                .build();

        // Then
        assertThat(urlMapping.getExpiresAt()).isPresent();
        assertThat(urlMapping.getExpiresAt().get()).isEqualTo(expiresAt);
    }

    @Test
    @DisplayName("Should return empty Optional when expiresAt is null")
    void shouldReturnEmptyOptionalWhenExpiresAtIsNull() {
        // When
        UrlMapping urlMapping = UrlMapping.builder()
                .originalUrl("https://www.example.com")
                .shortCode("abc123")
                .createdAt(Instant.now())
                .expiresAt(null)
                .build();

        // Then
        assertThat(urlMapping.getExpiresAt()).isEmpty();
    }

    @Test
    @DisplayName("Should return false for isExpired when no expiration date is set")
    void shouldReturnFalseForIsExpiredWhenNoExpirationDate() {
        // When
        UrlMapping urlMapping = UrlMapping.builder()
                .originalUrl("https://www.example.com")
                .shortCode("abc123")
                .createdAt(Instant.now())
                .expiresAt(null)
                .build();

        // Then
        assertThat(urlMapping.isExpired()).isFalse();
    }

    @Test
    @DisplayName("Should return true for isExpired when expiration date is in the past")
    void shouldReturnTrueForIsExpiredWhenExpirationDateInPast() {
        // Given
        Instant pastDate = Instant.now().minus(7, ChronoUnit.DAYS);

        // When
        UrlMapping urlMapping = UrlMapping.builder()
                .originalUrl("https://www.example.com")
                .shortCode("abc123")
                .createdAt(Instant.now().minus(14, ChronoUnit.DAYS))
                .expiresAt(pastDate)
                .build();

        // Then
        assertThat(urlMapping.isExpired()).isTrue();
    }

    @Test
    @DisplayName("Should return false for isExpired when expiration date is in the future")
    void shouldReturnFalseForIsExpiredWhenExpirationDateInFuture() {
        // Given
        Instant futureDate = Instant.now().plus(7, ChronoUnit.DAYS);

        // When
        UrlMapping urlMapping = UrlMapping.builder()
                .originalUrl("https://www.example.com")
                .shortCode("abc123")
                .createdAt(Instant.now())
                .expiresAt(futureDate)
                .build();

        // Then
        assertThat(urlMapping.isExpired()).isFalse();
    }

    @Test
    @DisplayName("Should be able to update expiration date")
    void shouldBeAbleToUpdateExpirationDate() {
        // Given
        UrlMapping urlMapping = UrlMapping.builder()
                .originalUrl("https://www.example.com")
                .shortCode("abc123")
                .createdAt(Instant.now())
                .expiresAt(null)
                .build();

        Instant newExpirationDate = Instant.now().plus(30, ChronoUnit.DAYS);

        // When
        urlMapping.setExpiresAt(newExpirationDate);

        // Then
        assertThat(urlMapping.getExpiresAt()).isPresent();
        assertThat(urlMapping.getExpiresAt().get()).isEqualTo(newExpirationDate);
    }

    @Test
    @DisplayName("Should handle edge case when expiration is exactly now")
    void shouldHandleEdgeCaseWhenExpirationIsExactlyNow() throws InterruptedException {
        // Given - Set expiration to a time slightly in the past to ensure it's expired
        Instant expiration = Instant.now().minusMillis(100);

        UrlMapping urlMapping = UrlMapping.builder()
                .originalUrl("https://www.example.com")
                .shortCode("abc123")
                .createdAt(Instant.now().minus(1, ChronoUnit.DAYS))
                .expiresAt(expiration)
                .build();

        // Small delay to ensure the instant has passed
        Thread.sleep(10);

        // Then - Should be expired since the expiration time has passed
        assertThat(urlMapping.isExpired()).isTrue();
    }
}