package com.ankur.security.saml.controller;

import com.ankur.security.saml.model.IdpCertificate;
import com.ankur.security.saml.service.CertificateService;
import com.ankur.security.saml.service.TenantSamlConfigService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;

/**
 * REST API for managing IdP certificates per tenant.
 *
 * UC-S06: cert CRUD API (upload, validate, remove).
 * UC-S07: manual cert rotation trigger from federationMetadataUrl.
 *
 * Multiple certs can be active simultaneously to support zero-downtime rotation.
 */
@RestController
@RequestMapping("/api/tenants/{tenantId}/saml/certificates")
public class CertificateController {

    private final TenantSamlConfigService configService;
    private final CertificateService certificateService;

    public CertificateController(TenantSamlConfigService configService,
                                 CertificateService certificateService) {
        this.configService = configService;
        this.certificateService = certificateService;
    }

    /**
     * GET /api/tenants/{tenantId}/saml/certificates
     * List all IdP certificates stored for a tenant.
     */
    @GetMapping
    public ResponseEntity<List<IdpCertificate>> listCertificates(@PathVariable String tenantId) {
        return ResponseEntity.ok(configService.listCertificates(tenantId));
    }

    /**
     * POST /api/tenants/{tenantId}/saml/certificates
     * Upload a new IdP public key certificate.
     *
     * Validates the PEM and parses metadata (issuer, subject, valid range, thumbprint)
     * before storing. Rejects invalid or malformed certificates with 400.
     *
     * Multiple certs can coexist — old and new cert both active during rotation.
     *
     * Request body:
     * {
     *   "alias": "okta-primary-2025",
     *   "certificatePem": "-----BEGIN CERTIFICATE-----\n...\n-----END CERTIFICATE-----"
     * }
     */
    @PostMapping
    public ResponseEntity<IdpCertificate> addCertificate(
            @PathVariable String tenantId,
            @RequestBody IdpCertificate cert) {
        IdpCertificate saved = configService.addCertificate(tenantId, cert);
        return ResponseEntity.status(HttpStatus.CREATED).body(saved);
    }

    /**
     * POST /api/tenants/{tenantId}/saml/certificates/{certId}/validate
     * Validate a certificate PEM without storing it.
     *
     * Returns parsed metadata if valid, or an error message if not.
     * Mirrors the SOE "Validate Certificate" button.
     */
    @PostMapping("/{certId}/validate")
    public ResponseEntity<CertificateService.CertificateValidationResult> validateCertificate(
            @PathVariable String tenantId,
            @PathVariable String certId,
            @RequestBody Map<String, String> body) {
        String pem = body.get("certificatePem");
        if (pem == null || pem.isBlank()) {
            return ResponseEntity.badRequest().build();
        }
        CertificateService.CertificateValidationResult result = certificateService.validate(pem);
        if (result.valid()) {
            return ResponseEntity.ok(result);
        }
        return ResponseEntity.unprocessableEntity().body(result);
    }

    /**
     * DELETE /api/tenants/{tenantId}/saml/certificates/{certId}
     * Remove an IdP certificate (expired or compromised).
     *
     * Warning: removing the last active cert will break SAML auth for this tenant.
     * Use /test to verify a replacement cert is active before removing the old one.
     */
    @DeleteMapping("/{certId}")
    public ResponseEntity<Void> removeCertificate(
            @PathVariable String tenantId,
            @PathVariable String certId) {
        configService.removeCertificate(tenantId, certId);
        return ResponseEntity.noContent().build();
    }

    /**
     * POST /api/tenants/{tenantId}/saml/certificates/refresh
     * UC-S07: manually trigger cert refresh from federationMetadataUrl.
     *
     * Normally runs hourly via @Scheduled. Use this to trigger immediately
     * (e.g. after IdP announces a cert rotation).
     *
     * Requires federationMetadataUrl to be set in the tenant's SAML config.
     * Azure Entra ID supports this; Okta/Google require manual cert upload.
     */
    @PostMapping("/refresh")
    public ResponseEntity<Map<String, Object>> refreshCertificates(@PathVariable String tenantId) {
        int refreshed = configService.refreshCertificatesFromMetadata(tenantId);
        return ResponseEntity.ok(Map.of(
            "tenantId", tenantId,
            "certsRefreshed", refreshed,
            "message", refreshed == 0
                ? "No new certificates found in federation metadata"
                : refreshed + " certificate(s) refreshed"
        ));
    }

    @ExceptionHandler(NoSuchElementException.class)
    public ResponseEntity<Map<String, String>> handleNotFound(NoSuchElementException ex) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(Map.of("error", ex.getMessage()));
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Map<String, String>> handleBadRequest(IllegalArgumentException ex) {
        return ResponseEntity.badRequest()
                .body(Map.of("error", ex.getMessage()));
    }

    @ExceptionHandler(IllegalStateException.class)
    public ResponseEntity<Map<String, String>> handleConflict(IllegalStateException ex) {
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(Map.of("error", ex.getMessage()));
    }
}