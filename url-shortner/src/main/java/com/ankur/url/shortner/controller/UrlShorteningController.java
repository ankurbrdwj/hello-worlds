package com.ankur.url.shortner.controller;

import com.ankur.url.shortner.dto.CreateShortUrlRequest;
import com.ankur.url.shortner.dto.ResolveUrlResponse;
import com.ankur.url.shortner.dto.ShortenedUrlReponse;
import com.ankur.url.shortner.entity.UrlMapping;
import com.ankur.url.shortner.service.UrlShortenerService;
import com.ankur.url.shortner.service.impl.UrlResolverService;
import jakarta.validation.Valid;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
public class UrlShorteningController {

    private final UrlShortenerService shortenerService;
    private final UrlResolverService resolverService;

    public UrlShorteningController(UrlShortenerService shortenerService,
                                   UrlResolverService resolverService) {
        this.shortenerService = shortenerService;
        this.resolverService = resolverService;
    }

    /**
     * Create a shortened URL
     * POST /shorten
     */
    @PostMapping("/shorten")
    public ResponseEntity<ShortenedUrlReponse> createShortUrl(
            @Valid @RequestBody CreateShortUrlRequest request) {

        UrlMapping urlMapping = shortenerService.createShortUrl(request);

        ShortenedUrlReponse response = new ShortenedUrlReponse(
                urlMapping.getOriginalUrl(),
                urlMapping.getShortCode(),
                urlMapping.getCustomAlias(),
                urlMapping.getExpiresAt().orElseGet(() -> null),
                urlMapping.getCreatedAt()
        );
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /**
     * Get original URL by short code (API response)
     * GET /api/urls/{shortCode}
     */
    @GetMapping("/{shortCode}")
    public ResponseEntity<ResolveUrlResponse> getOriginalUrl(@PathVariable String shortCode) {
        String originalUrl = resolverService.resolveUrl(shortCode);
        ResolveUrlResponse response = new ResolveUrlResponse(
                originalUrl,
                shortCode
        );
        return ResponseEntity.ok(response);
    }

}
