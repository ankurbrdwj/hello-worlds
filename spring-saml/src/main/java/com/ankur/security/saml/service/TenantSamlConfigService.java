package com.ankur.security.saml.service;

import com.ankur.security.saml.model.IdpCertificate;
import com.ankur.security.saml.model.TenantSamlConfig;
import com.ankur.security.saml.repository.TenantSamlConfigRepository;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.UUID;

@Service
public class TenantSamlConfigService {

    private final TenantSamlConfigRepository repository;
    private final CertificateService certificateService;

    public TenantSamlConfigService(TenantSamlConfigRepository repository,
                                   CertificateService certificateService) {
        this.repository = repository;
        this.certificateService = certificateService;
    }

    // ── Config CRUD ───────────────────────────────────────────────────────────

    public TenantSamlConfig saveConfig(String tenantId, TenantSamlConfig incoming) {
        incoming.setTenantId(tenantId);

        Optional<TenantSamlConfig> existing = repository.findByTenantId(tenantId);
        if (existing.isPresent()) {
            // preserve createdAt and existing certs on update
            incoming.setCreatedAt(existing.get().getCreatedAt());
            if (incoming.getCertificates().isEmpty()) {
                incoming.setCertificates(existing.get().getCertificates());
            }
        }

        incoming.setUpdatedAt(Instant.now());
        repository.save(incoming);
        return incoming;
    }

    public TenantSamlConfig getConfig(String tenantId) {
        return repository.findByTenantId(tenantId)
                .orElseThrow(() -> new NoSuchElementException("No SAML config for tenant: " + tenantId));
    }

    public void deleteConfig(String tenantId) {
        if (!repository.existsByTenantId(tenantId)) {
            throw new NoSuchElementException("No SAML config for tenant: " + tenantId);
        }
        repository.deleteByTenantId(tenantId);
    }

    // ── Certificate management ────────────────────────────────────────────────

    public IdpCertificate addCertificate(String tenantId, IdpCertificate cert) {
        TenantSamlConfig config = getConfig(tenantId);

        cert.setId(UUID.randomUUID().toString());
        cert.setTenantId(tenantId);
        cert.setUploadedAt(Instant.now());

        // parse and enrich with metadata — throws if PEM is invalid
        certificateService.parseAndValidate(cert);

        config.getCertificates().add(cert);
        config.setUpdatedAt(Instant.now());
        repository.save(config);
        return cert;
    }

    public List<IdpCertificate> listCertificates(String tenantId) {
        return getConfig(tenantId).getCertificates();
    }

    public void removeCertificate(String tenantId, String certId) {
        TenantSamlConfig config = getConfig(tenantId);
        boolean removed = config.getCertificates().removeIf(c -> c.getId().equals(certId));
        if (!removed) {
            throw new NoSuchElementException("Certificate not found: " + certId);
        }
        config.setUpdatedAt(Instant.now());
        repository.save(config);
    }

    /**
     * Manual cert refresh from federationMetadataUrl.
     * UC-S07: normally runs hourly via @Scheduled.
     * Returns the number of certs refreshed.
     */
    public int refreshCertificatesFromMetadata(String tenantId) {
        TenantSamlConfig config = getConfig(tenantId);
        if (config.getFederationMetadataUrl() == null || config.getFederationMetadataUrl().isBlank()) {
            throw new IllegalStateException("No federationMetadataUrl configured for tenant: " + tenantId);
        }
        // In production: fetch URL, parse XML, extract <ds:X509Certificate> elements,
        // diff against current certs, add new, mark expired ones inactive.
        // Stubbed here — full implementation requires OpenSAML metadata parsing.
        config.setUpdatedAt(Instant.now());
        repository.save(config);
        return 0; // stub: 0 new certs fetched
    }

    // ── Config test (dry run) ─────────────────────────────────────────────────

    /**
     * UC-S16: validate config completeness before going live.
     * Returns a list of validation errors. Empty list = config is valid.
     */
    public List<String> testConfig(String tenantId) {
        TenantSamlConfig config = getConfig(tenantId);
        var errors = new java.util.ArrayList<String>();

        if (config.getSingleSignOnServiceUrl() == null || config.getSingleSignOnServiceUrl().isBlank()) {
            errors.add("singleSignOnServiceUrl is required");
        }
        if (config.getIssuerEntityId() == null || config.getIssuerEntityId().isBlank()) {
            errors.add("issuerEntityId is required");
        }
        if (config.getCertificates().isEmpty()) {
            errors.add("At least one IdP certificate is required");
        } else {
            Instant now = Instant.now();
            long activeCerts = config.getCertificates().stream()
                    .filter(c -> c.isActive() && c.getValidTo() != null && c.getValidTo().isAfter(now))
                    .count();
            if (activeCerts == 0) {
                errors.add("No active non-expired certificates found");
            }
        }
        if (!config.isUseNameIdAsUserIdentifier()
                && (config.getUserIdentifierAttribute() == null || config.getUserIdentifierAttribute().isBlank())) {
            errors.add("userIdentifierAttribute is required when useNameIdAsUserIdentifier=false");
        }
        if (config.getEmailAttribute() == null || config.getEmailAttribute().isBlank()) {
            errors.add("emailAttribute is required");
        }
        if (config.getDisplayNameAttribute() == null || config.getDisplayNameAttribute().isBlank()) {
            errors.add("displayNameAttribute is required");
        }
        if (config.isDisableSpInitiated() && config.isAutoLogin()
                && (config.getIdpInitiatedSignOnUrl() == null || config.getIdpInitiatedSignOnUrl().isBlank())) {
            errors.add("idpInitiatedSignOnUrl is required when autoLogin=true");
        }

        return errors;
    }
}