package com.ankur.url.shortner.service.impl;

import com.ankur.url.shortner.dto.ValidationResult;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.*;

@DisplayName("AlphanumericAliasValidator Tests")
class AlphanumericAliasValidatorTest {

    private AlphanumericAliasValidator validator;

    @BeforeEach
    void setUp() {
        validator = new AlphanumericAliasValidator();
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "abc",
            "test123",
            "my-alias",
            "my_alias",
            "test-alias_123",
            "UPPERCASE",
            "MixedCase123",
            "aaaaaaaaaaaaaaaaaaaa"
    })
    @DisplayName("Should validate valid aliases")
    void shouldValidateValidAliases(String alias) {
        // When
        ValidationResult result = validator.validate(alias);

        // Then
        assertThat(result.isValid()).isTrue();
        assertThat(result.getErrorMessage()).isEmpty();
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {"   ", "\t", "\n"})
    @DisplayName("Should invalidate null, empty or blank aliases")
    void shouldInvalidateNullEmptyOrBlankAliases(String alias) {
        // When
        ValidationResult result = validator.validate(alias);

        // Then
        assertThat(result.isValid()).isFalse();
        assertThat(result.getErrorMessage()).isPresent();
        assertThat(result.getErrorMessage().get()).isEqualTo("Alias cannot be empty");
    }

    @ParameterizedTest
    @ValueSource(strings = {"a", "ab", "12"})
    @DisplayName("Should invalidate aliases shorter than minimum length")
    void shouldInvalidateAliasesShorterThanMinimumLength(String alias) {
        // When
        ValidationResult result = validator.validate(alias);

        // Then
        assertThat(result.isValid()).isFalse();
        assertThat(result.getErrorMessage()).isPresent();
        assertThat(result.getErrorMessage().get()).isEqualTo("Alias must be at least 3 characters");
    }

    @Test
    @DisplayName("Should invalidate alias longer than maximum length")
    void shouldInvalidateAliasLongerThanMaximumLength() {
        // Given
        String longAlias = "a".repeat(21);

        // When
        ValidationResult result = validator.validate(longAlias);

        // Then
        assertThat(result.isValid()).isFalse();
        assertThat(result.getErrorMessage()).isPresent();
        assertThat(result.getErrorMessage().get()).isEqualTo("Alias cannot exceed 20 characters");
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "test@alias",
            "test alias",
            "test.alias",
            "test#alias",
            "test$alias",
            "test%alias",
            "test&alias",
            "test*alias",
            "test+alias",
            "test=alias",
            "test!alias"
    })
    @DisplayName("Should invalidate aliases with invalid characters")
    void shouldInvalidateAliasesWithInvalidCharacters(String alias) {
        // When
        ValidationResult result = validator.validate(alias);

        // Then
        assertThat(result.isValid()).isFalse();
        assertThat(result.getErrorMessage()).isPresent();
        assertThat(result.getErrorMessage().get())
                .isEqualTo("Alias can only contain letters, numbers, hyphens, and underscores");
    }

    @Test
    @DisplayName("Should validate alias at minimum length boundary")
    void shouldValidateAliasAtMinimumLengthBoundary() {
        // Given
        String minLengthAlias = "abc";

        // When
        ValidationResult result = validator.validate(minLengthAlias);

        // Then
        assertThat(result.isValid()).isTrue();
    }

    @Test
    @DisplayName("Should validate alias at maximum length boundary")
    void shouldValidateAliasAtMaximumLengthBoundary() {
        // Given
        String maxLengthAlias = "a".repeat(20);

        // When
        ValidationResult result = validator.validate(maxLengthAlias);

        // Then
        assertThat(result.isValid()).isTrue();
    }
}