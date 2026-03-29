package com.ankur.security.saml.model;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Per-tenant SAML 2.0 configuration stored by KAYAK for Business.
 * One record per enterprise customer (tenant).
 *
 * Maps to all settings from the SOE SAML configuration screen.
 */
public class TenantSamlConfig {

    @NotBlank
    private String tenantId;

    @NotNull
    private IdpType idpType;

    // ── IdP endpoint ──────────────────────────────────────────────────────────

    /** IdP SSO login URL — sent authentication requests here. */
    @NotBlank
    private String singleSignOnServiceUrl;

    /** SP EntityID sent to IdP as Issuer. */
    @NotBlank
    private String issuerEntityId;

    /**
     * Audience restriction — usually same as issuerEntityId.
     * Defaults to issuerEntityId if not set.
     */
    private String audienceRestriction;

    // ── Protocol settings ─────────────────────────────────────────────────────

    /**
     * Enforce 80-byte max on RelayState.
     * UC-S02: required for certain legacy IdPs.
     */
    private boolean enforceRelayStateMaxLength;

    /**
     * SP-initiated disabled — redirect user to IdP directly, no AuthnRequest.
     * UC-S19.
     */
    private boolean disableSpInitiated;

    /** IdP login URL shown to user when SP-initiated is disabled. */
    private String idpInitiatedSignOnUrl;

    /** Auto-redirect to IdP (no prompt shown). Only valid when disableSpInitiated=true. */
    private boolean autoLogin;

    /**
     * ForceAuthn — IdP must re-authenticate even if session exists.
     * UC-S09.
     */
    private boolean forceReauthentication;

    /**
     * SubjectConfirmation validation toggle.
     * UC-S18: some IdPs don't send it.
     */
    private boolean verifySubjectConfirmation;

    // ── User identifier ───────────────────────────────────────────────────────

    /**
     * Use NameID as user identifier (default).
     * UC-S03: set false to use a custom assertion attribute instead.
     */
    private boolean useNameIdAsUserIdentifier = true;

    /**
     * When useNameIdAsUserIdentifier=false, this assertion attribute name
     * holds the unique user ID (e.g. ObjectGUID, employeeId).
     */
    private String userIdentifierAttribute;

    // ── Assertion attribute mapping ───────────────────────────────────────────

    /**
     * Attribute name in SAML assertion that holds the user's display name.
     * UC-S04: Azure uses long URI, Okta uses "displayName".
     */
    private String displayNameAttribute;

    /** Attribute name for email address. */
    private String emailAttribute;

    /** Attribute name for job title (optional). Synced on every login. */
    private String jobTitleAttribute;

    /** Attribute name for department (optional). Synced on every login. */
    private String departmentAttribute;

    /**
     * Attribute name for external company employee ID (optional).
     * UC-S13: stored alongside internal userId for reporting/audit.
     */
    private String externalIdAttribute;

    /**
     * Raw IdP → internal field name translation dict.
     * UC-S04: Azure sends http://schemas.xmlsoap.org/ws/2005/05/identity/claims/emailaddress
     *         Okta/Google send "email".
     * Key = IdP claim name, Value = internal field name.
     */
    private Map<String, String> attributeMapping = new HashMap<>();

    // ── Profile image ─────────────────────────────────────────────────────────

    /** UC-S12: download and store profile image on first user creation. */
    private boolean enableProfileImageImport;

    /** Assertion attribute containing the profile image URL. */
    private String profileImageUrlAttribute;

    /** Assertion attribute containing the Base64-encoded image (takes precedence). */
    private String profileImageBase64Attribute;

    // ── Signing ───────────────────────────────────────────────────────────────

    /** Sign outgoing AuthnRequests with SP private key. */
    private boolean signOutgoingAuthnRequests;

    /** Digest algorithm for signing (SHA256 recommended). */
    private DigestMethod signingDigestMethod = DigestMethod.SHA256;

    // ── Certificate management ────────────────────────────────────────────────

    /**
     * Federation metadata URL for hourly auto cert rotation.
     * UC-S07: Azure supports this, Okta/Google require manual rotation.
     */
    private String federationMetadataUrl;

    /**
     * IdP public key certificates used to verify assertion signatures.
     * Multiple certs supported for zero-downtime rotation.
     * UC-S06.
     */
    private List<IdpCertificate> certificates = new ArrayList<>();

    // ── Debug / compatibility ─────────────────────────────────────────────────

    /** UC-S14: enable SAML troubleshooting page (admin only, short-lived). */
    private boolean enableTroubleshootingPage;

    /** UC-S15: log raw SAML XML responses to DB. */
    private boolean enableResponseLogging;

    /** UC-S17: add UTF-8 BOM to EntityDescriptor.xml for legacy IdP compat. */
    private boolean addUtf8Bom;

    // ── Audit ─────────────────────────────────────────────────────────────────

    private Instant createdAt;
    private Instant updatedAt;

    public TenantSamlConfig() {
        this.createdAt = Instant.now();
        this.updatedAt = Instant.now();
    }

    // ── Getters / Setters ─────────────────────────────────────────────────────

    public String getTenantId() { return tenantId; }
    public void setTenantId(String tenantId) { this.tenantId = tenantId; }

    public IdpType getIdpType() { return idpType; }
    public void setIdpType(IdpType idpType) { this.idpType = idpType; }

    public String getSingleSignOnServiceUrl() { return singleSignOnServiceUrl; }
    public void setSingleSignOnServiceUrl(String singleSignOnServiceUrl) { this.singleSignOnServiceUrl = singleSignOnServiceUrl; }

    public String getIssuerEntityId() { return issuerEntityId; }
    public void setIssuerEntityId(String issuerEntityId) { this.issuerEntityId = issuerEntityId; }

    public String getAudienceRestriction() {
        return audienceRestriction != null ? audienceRestriction : issuerEntityId;
    }
    public void setAudienceRestriction(String audienceRestriction) { this.audienceRestriction = audienceRestriction; }

    public boolean isEnforceRelayStateMaxLength() { return enforceRelayStateMaxLength; }
    public void setEnforceRelayStateMaxLength(boolean enforceRelayStateMaxLength) { this.enforceRelayStateMaxLength = enforceRelayStateMaxLength; }

    public boolean isDisableSpInitiated() { return disableSpInitiated; }
    public void setDisableSpInitiated(boolean disableSpInitiated) { this.disableSpInitiated = disableSpInitiated; }

    public String getIdpInitiatedSignOnUrl() { return idpInitiatedSignOnUrl; }
    public void setIdpInitiatedSignOnUrl(String idpInitiatedSignOnUrl) { this.idpInitiatedSignOnUrl = idpInitiatedSignOnUrl; }

    public boolean isAutoLogin() { return autoLogin; }
    public void setAutoLogin(boolean autoLogin) { this.autoLogin = autoLogin; }

    public boolean isForceReauthentication() { return forceReauthentication; }
    public void setForceReauthentication(boolean forceReauthentication) { this.forceReauthentication = forceReauthentication; }

    public boolean isVerifySubjectConfirmation() { return verifySubjectConfirmation; }
    public void setVerifySubjectConfirmation(boolean verifySubjectConfirmation) { this.verifySubjectConfirmation = verifySubjectConfirmation; }

    public boolean isUseNameIdAsUserIdentifier() { return useNameIdAsUserIdentifier; }
    public void setUseNameIdAsUserIdentifier(boolean useNameIdAsUserIdentifier) { this.useNameIdAsUserIdentifier = useNameIdAsUserIdentifier; }

    public String getUserIdentifierAttribute() { return userIdentifierAttribute; }
    public void setUserIdentifierAttribute(String userIdentifierAttribute) { this.userIdentifierAttribute = userIdentifierAttribute; }

    public String getDisplayNameAttribute() { return displayNameAttribute; }
    public void setDisplayNameAttribute(String displayNameAttribute) { this.displayNameAttribute = displayNameAttribute; }

    public String getEmailAttribute() { return emailAttribute; }
    public void setEmailAttribute(String emailAttribute) { this.emailAttribute = emailAttribute; }

    public String getJobTitleAttribute() { return jobTitleAttribute; }
    public void setJobTitleAttribute(String jobTitleAttribute) { this.jobTitleAttribute = jobTitleAttribute; }

    public String getDepartmentAttribute() { return departmentAttribute; }
    public void setDepartmentAttribute(String departmentAttribute) { this.departmentAttribute = departmentAttribute; }

    public String getExternalIdAttribute() { return externalIdAttribute; }
    public void setExternalIdAttribute(String externalIdAttribute) { this.externalIdAttribute = externalIdAttribute; }

    public Map<String, String> getAttributeMapping() { return attributeMapping; }
    public void setAttributeMapping(Map<String, String> attributeMapping) { this.attributeMapping = attributeMapping; }

    public boolean isEnableProfileImageImport() { return enableProfileImageImport; }
    public void setEnableProfileImageImport(boolean enableProfileImageImport) { this.enableProfileImageImport = enableProfileImageImport; }

    public String getProfileImageUrlAttribute() { return profileImageUrlAttribute; }
    public void setProfileImageUrlAttribute(String profileImageUrlAttribute) { this.profileImageUrlAttribute = profileImageUrlAttribute; }

    public String getProfileImageBase64Attribute() { return profileImageBase64Attribute; }
    public void setProfileImageBase64Attribute(String profileImageBase64Attribute) { this.profileImageBase64Attribute = profileImageBase64Attribute; }

    public boolean isSignOutgoingAuthnRequests() { return signOutgoingAuthnRequests; }
    public void setSignOutgoingAuthnRequests(boolean signOutgoingAuthnRequests) { this.signOutgoingAuthnRequests = signOutgoingAuthnRequests; }

    public DigestMethod getSigningDigestMethod() { return signingDigestMethod; }
    public void setSigningDigestMethod(DigestMethod signingDigestMethod) { this.signingDigestMethod = signingDigestMethod; }

    public String getFederationMetadataUrl() { return federationMetadataUrl; }
    public void setFederationMetadataUrl(String federationMetadataUrl) { this.federationMetadataUrl = federationMetadataUrl; }

    public List<IdpCertificate> getCertificates() { return certificates; }
    public void setCertificates(List<IdpCertificate> certificates) { this.certificates = certificates; }

    public boolean isEnableTroubleshootingPage() { return enableTroubleshootingPage; }
    public void setEnableTroubleshootingPage(boolean enableTroubleshootingPage) { this.enableTroubleshootingPage = enableTroubleshootingPage; }

    public boolean isEnableResponseLogging() { return enableResponseLogging; }
    public void setEnableResponseLogging(boolean enableResponseLogging) { this.enableResponseLogging = enableResponseLogging; }

    public boolean isAddUtf8Bom() { return addUtf8Bom; }
    public void setAddUtf8Bom(boolean addUtf8Bom) { this.addUtf8Bom = addUtf8Bom; }

    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }

    public Instant getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Instant updatedAt) { this.updatedAt = updatedAt; }
}