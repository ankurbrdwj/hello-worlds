package com.ankur.url.shortner.repository;

import com.ankur.url.shortner.entity.UrlMapping;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;

@DataJpaTest
@DisplayName("UrlRepository Integration Tests")
class UrlRepositoryTest {

    @Autowired
    private UrlRepository repository;

    @Autowired
    private TestEntityManager entityManager;

    @Test
    @DisplayName("Should save and find URL mapping by short code")
    void shouldSaveAndFindUrlMappingByShortCode() {
        // Given
        UrlMapping urlMapping = UrlMapping.builder()
                .originalUrl("https://www.example.com")
                .shortCode("abc123")
                .createdAt(Instant.now())
                .build();

        // When
        UrlMapping saved = repository.save(urlMapping);
        entityManager.flush();
        Optional<UrlMapping> found = repository.findByShortCode("abc123");

        // Then
        assertThat(found).isPresent();
        assertThat(found.get().getOriginalUrl()).isEqualTo("https://www.example.com");
        assertThat(found.get().getShortCode()).isEqualTo("abc123");
    }

    @Test
    @DisplayName("Should return empty optional when short code not found")
    void shouldReturnEmptyOptionalWhenShortCodeNotFound() {
        // When
        Optional<UrlMapping> found = repository.findByShortCode("nonexistent");

        // Then
        assertThat(found).isEmpty();
    }

    @Test
    @DisplayName("Should return true when short code exists")
    void shouldReturnTrueWhenShortCodeExists() {
        // Given
        UrlMapping urlMapping = UrlMapping.builder()
                .originalUrl("https://www.example.com")
                .shortCode("exists")
                .createdAt(Instant.now())
                .build();
        repository.save(urlMapping);
        entityManager.flush();

        // When
        boolean exists = repository.existsByShortCode("exists");

        // Then
        assertThat(exists).isTrue();
    }

    @Test
    @DisplayName("Should return false when short code does not exist")
    void shouldReturnFalseWhenShortCodeDoesNotExist() {
        // When
        boolean exists = repository.existsByShortCode("nonexistent");

        // Then
        assertThat(exists).isFalse();
    }

    @Test
    @DisplayName("Should save URL mapping with custom alias")
    void shouldSaveUrlMappingWithCustomAlias() {
        // Given
        UrlMapping urlMapping = UrlMapping.builder()
                .originalUrl("https://www.example.com")
                .shortCode("myalias")
                .customAlias("myalias")
                .createdAt(Instant.now())
                .build();

        // When
        repository.save(urlMapping);
        entityManager.flush();
        Optional<UrlMapping> found = repository.findByShortCode("myalias");

        // Then
        assertThat(found).isPresent();
        assertThat(found.get().getCustomAlias()).isEqualTo("myalias");
    }

    @Test
    @DisplayName("Should save URL mapping with expiration date")
    void shouldSaveUrlMappingWithExpirationDate() {
        // Given
        Instant expiresAt = Instant.now().plus(7, ChronoUnit.DAYS);
        UrlMapping urlMapping = UrlMapping.builder()
                .originalUrl("https://www.example.com")
                .shortCode("expiring")
                .createdAt(Instant.now())
                .expiresAt(expiresAt)
                .build();

        // When
        repository.save(urlMapping);
        entityManager.flush();
        Optional<UrlMapping> found = repository.findByShortCode("expiring");

        // Then
        assertThat(found).isPresent();
        assertThat(found.get().getExpiresAt()).isPresent();
        assertThat(found.get().getExpiresAt().get()).isEqualTo(expiresAt);
    }

    @Test
    @DisplayName("Should persist created timestamp")
    void shouldPersistCreatedTimestamp() {
        // Given
        Instant createdAt = Instant.now();
        UrlMapping urlMapping = UrlMapping.builder()
                .originalUrl("https://www.example.com")
                .shortCode("timestamped")
                .createdAt(createdAt)
                .build();

        // When
        repository.save(urlMapping);
        entityManager.flush();
        Optional<UrlMapping> found = repository.findByShortCode("timestamped");

        // Then
        assertThat(found).isPresent();
        assertThat(found.get().getCreatedAt()).isNotNull();
    }

    @Test
    @DisplayName("Should update expiration date of existing URL mapping")
    void shouldUpdateExpirationDateOfExistingUrlMapping() {
        // Given
        UrlMapping urlMapping = UrlMapping.builder()
                .originalUrl("https://www.example.com")
                .shortCode("updateable")
                .createdAt(Instant.now())
                .expiresAt(null)
                .build();
        repository.save(urlMapping);
        entityManager.flush();

        // When
        Instant newExpirationDate = Instant.now().plus(30, ChronoUnit.DAYS);
        UrlMapping found = repository.findByShortCode("updateable").get();
        found.setExpiresAt(newExpirationDate);
        repository.save(found);
        entityManager.flush();

        // Then
        UrlMapping updated = repository.findByShortCode("updateable").get();
        assertThat(updated.getExpiresAt()).isPresent();
        assertThat(updated.getExpiresAt().get()).isEqualTo(newExpirationDate);
    }
}