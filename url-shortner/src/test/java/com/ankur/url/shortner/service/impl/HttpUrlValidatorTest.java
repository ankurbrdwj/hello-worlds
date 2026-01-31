package com.ankur.url.shortner.service.impl;

import com.ankur.url.shortner.dto.ValidationResult;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.*;

@DisplayName("HttpUrlValidator Tests")
class HttpUrlValidatorTest {

    private HttpUrlValidator validator;

    @BeforeEach
    void setUp() {
        validator = new HttpUrlValidator();
    }

    @Test
    @DisplayName("Should validate valid HTTP URL")
    void shouldValidateValidHttpUrl() {
        // Given
        String url = "http://www.example.com";

        // When
        ValidationResult result = validator.validate(url);

        // Then
        assertThat(result.isValid()).isTrue();
        assertThat(result.getErrorMessage()).isEmpty();
    }

    @Test
    @DisplayName("Should validate valid HTTPS URL")
    void shouldValidateValidHttpsUrl() {
        // Given
        String url = "https://www.example.com";

        // When
        ValidationResult result = validator.validate(url);

        // Then
        assertThat(result.isValid()).isTrue();
        assertThat(result.getErrorMessage()).isEmpty();
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "https://www.example.com/path/to/resource",
            "http://example.com:8080",
            "https://subdomain.example.com",
            "http://example.com/path?query=param&foo=bar",
            "https://example.com/path#fragment"
    })
    @DisplayName("Should validate various valid URL formats")
    void shouldValidateVariousValidUrlFormats(String url) {
        // When
        ValidationResult result = validator.validate(url);

        // Then
        assertThat(result.isValid()).isTrue();
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {"   ", "\t", "\n"})
    @DisplayName("Should invalidate null, empty or blank URLs")
    void shouldInvalidateNullEmptyOrBlankUrls(String url) {
        // When
        ValidationResult result = validator.validate(url);

        // Then
        assertThat(result.isValid()).isFalse();
        assertThat(result.getErrorMessage()).isPresent();
        assertThat(result.getErrorMessage().get()).isEqualTo("URL cannot be empty");
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "ftp://example.com",
            "example.com",
            "www.example.com",
            "//example.com",
            "htp://example.com"
    })
    @DisplayName("Should invalidate URLs not starting with http:// or https://")
    void shouldInvalidateUrlsNotStartingWithHttpOrHttps(String url) {
        // When
        ValidationResult result = validator.validate(url);

        // Then
        assertThat(result.isValid()).isFalse();
        assertThat(result.getErrorMessage()).isPresent();
        assertThat(result.getErrorMessage().get()).isEqualTo("URL must start with http:// or https://");
    }

    @Test
    @DisplayName("Should invalidate URL exceeding maximum length")
    void shouldInvalidateUrlExceedingMaximumLength() {
        // Given
        String longUrl = "https://example.com/" + "a".repeat(2100);

        // When
        ValidationResult result = validator.validate(longUrl);

        // Then
        assertThat(result.isValid()).isFalse();
        assertThat(result.getErrorMessage()).isPresent();
        assertThat(result.getErrorMessage().get()).isEqualTo("URL exceeds maximum length");
    }

    @Test
    @DisplayName("Should validate URL at maximum length boundary")
    void shouldValidateUrlAtMaximumLengthBoundary() {
        // Given - URL exactly at 2048 characters
        String exactLengthUrl = "https://example.com/" + "a".repeat(2028);

        // When
        ValidationResult result = validator.validate(exactLengthUrl);

        // Then
        assertThat(result.isValid()).isTrue();
    }
}