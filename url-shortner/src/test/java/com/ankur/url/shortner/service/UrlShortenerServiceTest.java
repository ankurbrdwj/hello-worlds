package com.ankur.url.shortner.service;

import com.ankur.url.shortner.dto.CreateShortUrlRequest;
import com.ankur.url.shortner.dto.ValidationResult;
import com.ankur.url.shortner.entity.UrlMapping;
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
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("UrlShortenerService Tests")
class UrlShortenerServiceTest {

    @Mock
    private UrlRepository repository;

    @Mock
    private ShortCodeGenerator generator;

    @Mock
    private UrlValidator urlValidator;

    @Mock
    private AliasValidator aliasValidator;

    @InjectMocks
    private UrlShortenerService service;

    private CreateShortUrlRequest validRequest;
    private ValidationResult validResult;

    @BeforeEach
    void setUp() {
        validRequest = new CreateShortUrlRequest(
                "https://www.example.com",
                Optional.empty(),
                Optional.empty()
        );
        validResult = ValidationResult.valid();
    }

    @Test
    @DisplayName("Should create short URL with generated code when no custom alias provided")
    void shouldCreateShortUrlWithGeneratedCode() {
        // Given
        String generatedCode = "abc123";
        when(urlValidator.validate(anyString())).thenReturn(validResult);
        when(generator.generate()).thenReturn(generatedCode);
        when(repository.existsByShortCode(generatedCode)).thenReturn(false);
        when(repository.save(any(UrlMapping.class))).thenAnswer(i -> i.getArgument(0));

        // When
        UrlMapping result = service.createShortUrl(validRequest);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getShortCode()).isEqualTo(generatedCode);
        assertThat(result.getOriginalUrl()).isEqualTo("https://www.example.com");
        verify(generator).generate();
        verify(repository).save(any(UrlMapping.class));
    }

    @Test
    @DisplayName("Should create short URL with custom alias when provided and valid")
    void shouldCreateShortUrlWithCustomAlias() {
        // Given
        String customAlias = "myalias";
        CreateShortUrlRequest requestWithAlias = new CreateShortUrlRequest(
                "https://www.example.com",
                Optional.of(customAlias),
                Optional.empty()
        );
        when(urlValidator.validate(anyString())).thenReturn(validResult);
        when(aliasValidator.validate(customAlias)).thenReturn(validResult);
        when(repository.existsByShortCode(customAlias)).thenReturn(false);
        when(repository.save(any(UrlMapping.class))).thenAnswer(i -> i.getArgument(0));

        // When
        UrlMapping result = service.createShortUrl(requestWithAlias);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getShortCode()).isEqualTo(customAlias);
        verify(aliasValidator).validate(customAlias);
        verify(repository).existsByShortCode(customAlias);
        verify(repository).save(any(UrlMapping.class));
        verify(generator, never()).generate();
    }

    @Test
    @DisplayName("Should throw exception when URL validation fails")
    void shouldThrowExceptionWhenUrlValidationFails() {
        // Given
        ValidationResult invalidResult = ValidationResult.invalid("Invalid URL format");
        when(urlValidator.validate(anyString())).thenReturn(invalidResult);

        // When/Then
        assertThatThrownBy(() -> service.createShortUrl(validRequest))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Invalid URL format");

        verify(repository, never()).save(any());
    }

    @Test
    @DisplayName("Should throw exception when custom alias is invalid")
    void shouldThrowExceptionWhenAliasIsInvalid() {
        // Given
        String invalidAlias = "invalid@alias";
        CreateShortUrlRequest requestWithInvalidAlias = new CreateShortUrlRequest(
                "https://www.example.com",
                Optional.of(invalidAlias),
                Optional.empty()
        );
        ValidationResult invalidAliasResult = ValidationResult.invalid("Alias contains invalid characters");
        when(urlValidator.validate(anyString())).thenReturn(validResult);
        when(aliasValidator.validate(invalidAlias)).thenReturn(invalidAliasResult);

        // When/Then
        assertThatThrownBy(() -> service.createShortUrl(requestWithInvalidAlias))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Alias contains invalid characters");

        verify(repository, never()).save(any());
    }

    @Test
    @DisplayName("Should throw exception when custom alias already exists")
    void shouldThrowExceptionWhenAliasAlreadyExists() {
        // Given
        String existingAlias = "existing";
        CreateShortUrlRequest requestWithExistingAlias = new CreateShortUrlRequest(
                "https://www.example.com",
                Optional.of(existingAlias),
                Optional.empty()
        );
        when(urlValidator.validate(anyString())).thenReturn(validResult);
        when(aliasValidator.validate(existingAlias)).thenReturn(validResult);
        when(repository.existsByShortCode(existingAlias)).thenReturn(true);

        // When/Then
        assertThatThrownBy(() -> service.createShortUrl(requestWithExistingAlias))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Alias already exists");

        verify(repository, never()).save(any());
    }

    @Test
    @DisplayName("Should create short URL with expiration date when provided")
    void shouldCreateShortUrlWithExpirationDate() {
        // Given
        Instant expiresAt = Instant.now().plus(7, ChronoUnit.DAYS);
        CreateShortUrlRequest requestWithExpiration = new CreateShortUrlRequest(
                "https://www.example.com",
                Optional.empty(),
                Optional.of(expiresAt)
        );
        String generatedCode = "abc123";
        when(urlValidator.validate(anyString())).thenReturn(validResult);
        when(generator.generate()).thenReturn(generatedCode);
        when(repository.existsByShortCode(generatedCode)).thenReturn(false);
        when(repository.save(any(UrlMapping.class))).thenAnswer(i -> i.getArgument(0));

        // When
        UrlMapping result = service.createShortUrl(requestWithExpiration);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getExpiresAt()).isPresent();
        assertThat(result.getExpiresAt().get()).isEqualTo(expiresAt);
    }

    @Test
    @DisplayName("Should retry generation when short code collision occurs")
    void shouldRetryGenerationOnCollision() {
        // Given
        String firstCode = "abc123";
        String secondCode = "xyz789";
        when(urlValidator.validate(anyString())).thenReturn(validResult);
        when(generator.generate()).thenReturn(firstCode, secondCode);
        when(repository.existsByShortCode(firstCode)).thenReturn(true);
        when(repository.existsByShortCode(secondCode)).thenReturn(false);
        when(repository.save(any(UrlMapping.class))).thenAnswer(i -> i.getArgument(0));

        // When
        UrlMapping result = service.createShortUrl(validRequest);

        // Then
        assertThat(result.getShortCode()).isEqualTo(secondCode);
        verify(generator, times(2)).generate();
    }

    @Test
    @DisplayName("Should throw exception when unable to generate unique code after max attempts")
    void shouldThrowExceptionWhenMaxAttemptsReached() {
        // Given
        when(urlValidator.validate(anyString())).thenReturn(validResult);
        when(generator.generate()).thenReturn("collision");
        when(repository.existsByShortCode(anyString())).thenReturn(true);

        // When/Then
        assertThatThrownBy(() -> service.createShortUrl(validRequest))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("Failed to generate unique short code");

        verify(generator, times(11)).generate();
        verify(repository, never()).save(any());
    }
}