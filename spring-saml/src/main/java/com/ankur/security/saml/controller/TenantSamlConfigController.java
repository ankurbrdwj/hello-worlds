package com.ankur.security.saml.controller;

import com.ankur.security.saml.model.TenantSamlConfig;
import com.ankur.security.saml.service.TenantSamlConfigService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;

/**
 * REST API for managing per-tenant SAML configuration.
 *
 * Equivalent to the SOE "Configure SAML on Stack Internal Enterprise" admin screen,
 * but multi-tenant — one config per enterprise customer.
 *
 * In production, secure these endpoints to admin/platform roles only.
 */
@RestController
@RequestMapping("/api/tenants/{tenantId}/saml")
public class TenantSamlConfigController {

    private final TenantSamlConfigService configService;

    public TenantSamlConfigController(TenantSamlConfigService configService) {
        this.configService = configService;
    }

    /**
     * GET /api/tenants/{tenantId}/saml/config
     * Retrieve current SAML configuration for a tenant.
     */
    @GetMapping("/config")
    public ResponseEntity<TenantSamlConfig> getConfig(@PathVariable String tenantId) {
        return ResponseEntity.ok(configService.getConfig(tenantId));
    }

    /**
     * PUT /api/tenants/{tenantId}/saml/config
     * Create or update the SAML configuration for a tenant.
     *
     * Idempotent — safe to call multiple times. Preserves existing certificates
     * if none are included in the request body.
     *
     * Stores:
     * - IdP SSO URL
     * - Issuer / EntityID
     * - Protocol settings (RelayState, SP-initiated, ForceAuthn, SubjectConfirmation)
     * - User identifier strategy (NameID vs custom attribute)
     * - Assertion attribute names (displayName, email, jobTitle, department, externalId)
     * - Attribute mapping dict (IdP claim name → internal field name)
     * - Profile image config
     * - Signing config
     * - Debug toggles
     */
    @PutMapping("/config")
    public ResponseEntity<TenantSamlConfig> saveConfig(
            @PathVariable String tenantId,
            @RequestBody TenantSamlConfig config) {
        TenantSamlConfig saved = configService.saveConfig(tenantId, config);
        return ResponseEntity.ok(saved);
    }

    /**
     * DELETE /api/tenants/{tenantId}/saml/config
     * Remove the SAML configuration for a tenant (offboarding).
     */
    @DeleteMapping("/config")
    public ResponseEntity<Void> deleteConfig(@PathVariable String tenantId) {
        configService.deleteConfig(tenantId);
        return ResponseEntity.noContent().build();
    }

    /**
     * POST /api/tenants/{tenantId}/saml/test
     * UC-S16: dry-run the current config before going live.
     *
     * Validates completeness (certs present, required fields set, no expired certs).
     * Returns 200 with empty errors list if valid, or 422 with the list of problems.
     */
    @PostMapping("/test")
    public ResponseEntity<Map<String, Object>> testConfig(@PathVariable String tenantId) {
        List<String> errors = configService.testConfig(tenantId);
        if (errors.isEmpty()) {
            return ResponseEntity.ok(Map.of("valid", true, "errors", List.of()));
        }
        return ResponseEntity.unprocessableEntity()
                .body(Map.of("valid", false, "errors", errors));
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
}