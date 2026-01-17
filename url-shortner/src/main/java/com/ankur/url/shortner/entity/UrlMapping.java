package com.ankur.url.shortner.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.Instant;
import java.util.Optional;

@Data
@Builder
@Entity
@Table(name = "url_mapping")
@EqualsAndHashCode(callSuper = true)  // Include parent fields
public final class UrlMapping extends AbstractEntity {

    @Column(nullable = false)
    private final String originalUrl;
    @Column(nullable = false)
    private final String shortCode;
    @Column(nullable = true)
    private final String customAlias;
    @Column(nullable = true)
    private Instant expiresAt;
    @Column(nullable = false)
    private Instant createdAt;

    public Optional<Instant> getExpiresAt() {
        return Optional.ofNullable(expiresAt);
    }

    public void setExpiresAt(Instant expiresAt) {
        this.expiresAt = expiresAt;
    }

    public boolean isExpired() {
        return expiresAt != null && Instant.now().isAfter(expiresAt);
    }


}
