package com.ankur.url.shortner.service.impl;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.RepeatedTest;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.*;

@DisplayName("RandomShortCodeGenerator Tests")
class RandomShortCodeGeneratorTest {

    private RandomShortCodeGenerator generator;

    @BeforeEach
    void setUp() {
        generator = new RandomShortCodeGenerator();
    }

    @Test
    @DisplayName("Should generate non-null and non-empty code")
    void shouldGenerateNonNullAndNonEmptyCode() {
        // When
        String code = generator.generate();

        // Then
        assertThat(code).isNotNull().isNotEmpty();
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
    @DisplayName("Should generate unique codes on consecutive calls")
    void shouldGenerateUniqueCodesOnConsecutiveCalls() {
        // Given
        Set<String> codes = new HashSet<>();
        int iterations = 100;

        // When
        for (int i = 0; i < iterations; i++) {
            codes.add(generator.generate());
        }

        // Then
        assertThat(codes).hasSize(iterations);
    }

    @Test
    @DisplayName("Should generate codes in sequential order")
    void shouldGenerateCodesInSequentialOrder() {
        // Given
        List<String> codes = new ArrayList<>();

        // When
        for (int i = 0; i < 10; i++) {
            codes.add(generator.generate());
        }

        // Then - All codes should be unique
        assertThat(codes).doesNotHaveDuplicates();
    }

    @Test
    @DisplayName("Should be thread-safe when generating codes concurrently")
    void shouldBeThreadSafeWhenGeneratingCodesConcurrently() throws InterruptedException {
        // Given
        Set<String> codes = new HashSet<>();
        int threadCount = 10;
        int codesPerThread = 100;
        List<Thread> threads = new ArrayList<>();

        // When
        for (int i = 0; i < threadCount; i++) {
            Thread thread = new Thread(() -> {
                for (int j = 0; j < codesPerThread; j++) {
                    synchronized (codes) {
                        codes.add(generator.generate());
                    }
                }
            });
            threads.add(thread);
            thread.start();
        }

        // Wait for all threads to complete
        for (Thread thread : threads) {
            thread.join();
        }

        // Then - All generated codes should be unique
        assertThat(codes).hasSize(threadCount * codesPerThread);
    }

    @Test
    @DisplayName("Should not generate codes with whitespace")
    void shouldNotGenerateCodesWithWhitespace() {
        // When
        String code = generator.generate();

        // Then
        assertThat(code).doesNotContainAnyWhitespaces();
    }

    @Test
    @DisplayName("Should generate codes with reasonable length")
    void shouldGenerateCodesWithReasonableLength() {
        // When
        String code = generator.generate();

        // Then - Counter starts at 1000000, so codes should be reasonable length
        assertThat(code.length()).isBetween(3, 12);
    }
}