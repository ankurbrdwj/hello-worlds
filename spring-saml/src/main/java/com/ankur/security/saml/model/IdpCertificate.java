package com.ankur.security.saml.model;

import java.time.Instant;
import java.util.UUID;

public class IdpCertificate {

    private String id;
    private String tenantId;
    private String alias;
    private String certificatePem;

    // parsed metadata — populated by CertificateService
    private String issuer;
    private String subject;
    private Instant validFrom;
    private Instant validTo;
    private String thumbprint;
    private String signatureAlgorithm;

    private boolean active;
    private Instant uploadedAt;

    public IdpCertificate() {
        this.id = UUID.randomUUID().toString();
        this.active = true;
        this.uploadedAt = Instant.now();
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getTenantId() { return tenantId; }
    public void setTenantId(String tenantId) { this.tenantId = tenantId; }

    public String getAlias() { return alias; }
    public void setAlias(String alias) { this.alias = alias; }

    public String getCertificatePem() { return certificatePem; }
    public void setCertificatePem(String certificatePem) { this.certificatePem = certificatePem; }

    public String getIssuer() { return issuer; }
    public void setIssuer(String issuer) { this.issuer = issuer; }

    public String getSubject() { return subject; }
    public void setSubject(String subject) { this.subject = subject; }

    public Instant getValidFrom() { return validFrom; }
    public void setValidFrom(Instant validFrom) { this.validFrom = validFrom; }

    public Instant getValidTo() { return validTo; }
    public void setValidTo(Instant validTo) { this.validTo = validTo; }

    public String getThumbprint() { return thumbprint; }
    public void setThumbprint(String thumbprint) { this.thumbprint = thumbprint; }

    public String getSignatureAlgorithm() { return signatureAlgorithm; }
    public void setSignatureAlgorithm(String signatureAlgorithm) { this.signatureAlgorithm = signatureAlgorithm; }

    public boolean isActive() { return active; }
    public void setActive(boolean active) { this.active = active; }

    public Instant getUploadedAt() { return uploadedAt; }
    public void setUploadedAt(Instant uploadedAt) { this.uploadedAt = uploadedAt; }
}