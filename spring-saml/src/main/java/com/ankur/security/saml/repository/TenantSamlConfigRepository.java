package com.ankur.security.saml.repository;

import com.ankur.security.saml.model.TenantSamlConfig;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 * In-memory store for per-tenant SAML configurations.
 *
 * UC-S01: DB-backed in production — swap this with a JPA repository
 * backed by a tenants_saml_config table.
 */
@Repository
public class TenantSamlConfigRepository {

    private final ConcurrentHashMap<String, TenantSamlConfig> store = new ConcurrentHashMap<>();

    public void save(TenantSamlConfig config) {
        store.put(config.getTenantId(), config);
    }

    public Optional<TenantSamlConfig> findByTenantId(String tenantId) {
        return Optional.ofNullable(store.get(tenantId));
    }

    public Collection<TenantSamlConfig> findAll() {
        return store.values();
    }

    public boolean existsByTenantId(String tenantId) {
        return store.containsKey(tenantId);
    }

    public void deleteByTenantId(String tenantId) {
        store.remove(tenantId);
    }
}