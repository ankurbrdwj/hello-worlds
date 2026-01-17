package com.ankur.url.shortner.service;

import com.ankur.url.shortner.dto.CreateShortUrlRequest;
import com.ankur.url.shortner.dto.ValidationResult;
import com.ankur.url.shortner.entity.UrlMapping;
import com.ankur.url.shortner.repository.UrlRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.Instant;
@Service
public class UrlShortenerService {
    private final UrlRepository repository;
    private final ShortCodeGenerator generator;
    private final UrlValidator urlValidator;
    private final AliasValidator aliasValidator;

    @Autowired
    public UrlShortenerService(UrlRepository repository,
                               ShortCodeGenerator generator,
                               UrlValidator urlValidator,
                               AliasValidator aliasValidator) {
        this.repository = repository;
        this.generator = generator;
        this.urlValidator = urlValidator;
        this.aliasValidator = aliasValidator;
    }

    public UrlMapping createShortUrl(CreateShortUrlRequest request) {
        // Validate original URL
        ValidationResult urlValidation = urlValidator.validate(request.getOriginalUrl());
        if (!urlValidation.isValid()) {
            throw new IllegalArgumentException(urlValidation.getErrorMessage().orElse("Invalid URL"));
        }

        // Generate or validate short code
        String shortCode = request.getCustomAlias()
                .map(this::validateAndGetAlias)
                .orElseGet(this::generateUniqueShortCode);

        // Create and save shortened URL
        UrlMapping urlMapping = UrlMapping.builder()
                .originalUrl(request.getOriginalUrl())
                .shortCode(shortCode)
                .createdAt(Instant.now())
                .expiresAt(request.getExpiresAt().orElse(null))
                .build();

        repository.save(urlMapping);
        return urlMapping;
    }

    private String validateAndGetAlias(String alias) {
        ValidationResult validation = aliasValidator.validate(alias);
        if (!validation.isValid()) {
            throw new IllegalArgumentException(validation.getErrorMessage().orElse("Invalid alias"));
        }
        if (repository.existsByShortCode(alias)) {
            throw new IllegalArgumentException("Alias already exists");
        }
        return alias;
    }

    private String generateUniqueShortCode() {
        String shortCode;
        int attempts = 0;
        do {
            shortCode = generator.generate();
            if (++attempts > 10) {
                throw new RuntimeException("Failed to generate unique short code");
            }
        } while (repository.existsByShortCode(shortCode));
        return shortCode;
    }
}
