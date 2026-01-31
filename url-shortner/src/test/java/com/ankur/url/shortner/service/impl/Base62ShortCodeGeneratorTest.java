package com.ankur.url.shortner.service.impl;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.RepeatedTest;
import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.Set;

import static org.assertj.core.api.Assertions.*;

@DisplayName("Base62ShortCodeGenerator Tests")
class Base62ShortCodeGeneratorTest {

    private Base62ShortCodeGenerator generator;

    @BeforeEach
    void setUp() {
        generator = new Base62ShortCodeGenerator();
    }

    @Test
    @DisplayName("Should generate code with correct length")
    void shouldGenerateCodeWithCorrectLength() {
        // When
        String code = generator.generate();

        // Then
        assertThat(code).hasSize(7);
    }

    @RepeatedTest(10)
    @DisplayName("Should generate codes containing only valid Base62 characters")
    void shouldGenerateCodesContainingOnlyValidBase62Characters() {
        // When
        String code = generator.generate();

        // Then
        assertThat(code).matches("^[a-zA-Z0-9]+$");
    }

    @Test
    @DisplayName("Should generate different codes on consecutive calls")
    void shouldGenerateDifferentCodesOnConsecutiveCalls() {
        // When
        String code1 = generator.generate();
        String code2 = generator.generate();

        // Then - Note: there's a small probability they could be the same, but very unlikely
        assertThat(code1).isNotEqualTo(code2);
    }

    @Test
    @DisplayName("Should generate unique codes with high probability")
    void shouldGenerateUniqueCodesWithHighProbability() {
        // Given
        Set<String> generatedCodes = new HashSet<>();
        int iterations = 1000;

        // When
        for (int i = 0; i < iterations; i++) {
            generatedCodes.add(generator.generate());
        }

        // Then - Expect very high uniqueness (>99%)
        assertThat(generatedCodes).hasSizeGreaterThan(990);
    }

    @Test
    @DisplayName("Should not generate null or empty codes")
    void shouldNotGenerateNullOrEmptyCodes() {
        // When
        String code = generator.generate();

        // Then
        assertThat(code).isNotNull().isNotEmpty();
    }

    @RepeatedTest(5)
    @DisplayName("Should generate codes without whitespace")
    void shouldGenerateCodesWithoutWhitespace() {
        // When
        String code = generator.generate();

        // Then
        assertThat(code).doesNotContainAnyWhitespaces();
    }
}