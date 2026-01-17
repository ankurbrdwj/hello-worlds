package com.ankur.url.shortner.service.impl;

import com.ankur.url.shortner.entity.UrlMapping;
import com.ankur.url.shortner.exception.UrlExpiredException;
import com.ankur.url.shortner.exception.UrlNotFoundException;
import com.ankur.url.shortner.repository.UrlRepository;
import org.springframework.stereotype.Service;

@Service
public class UrlResolverService {

    private final UrlRepository repository;

    public UrlResolverService(UrlRepository repository) {
        this.repository = repository;
    }

    public String resolveUrl(String shortCode) {
        UrlMapping url = repository.findByShortCode(shortCode)
                .orElseThrow(() -> new UrlNotFoundException("Short URL not found: " + shortCode));

        if (url.isExpired()) {
            throw new UrlExpiredException("Short URL has expired: " + shortCode);
        }

        return url.getOriginalUrl();
    }
}
