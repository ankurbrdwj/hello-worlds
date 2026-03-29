package com.ankur.security.saml.saml;

import com.ankur.security.saml.model.IdpCertificate;
import com.ankur.security.saml.model.TenantSamlConfig;
import com.ankur.security.saml.repository.TenantSamlConfigRepository;
import org.springframework.security.saml2.core.Saml2X509Credential;
import org.springframework.security.saml2.provider.service.registration.RelyingPartyRegistration;
import org.springframework.security.saml2.provider.service.registration.RelyingPartyRegistrationRepository;
import org.springframework.security.saml2.provider.service.registration.Saml2MessageBinding;
import org.springframework.stereotype.Component;

import java.io.ByteArrayInputStream;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.util.Base64;
import java.util.List;

/**
 * UC-S01: DB-backed RelyingPartyRegistrationRepository.
 *
 * Spring Security calls this at runtime to look up the per-tenant
 * SAML configuration by registrationId (= tenantId).
 *
 * Each enterprise customer = one RelyingPartyRegistration = one row in DB.
 */
@Component
public class DynamicRelyingPartyRegistrationRepository implements RelyingPartyRegistrationRepository {

    private final TenantSamlConfigRepository configRepository;

    public DynamicRelyingPartyRegistrationRepository(TenantSamlConfigRepository configRepository) {
        this.configRepository = configRepository;
    }

    @Override
    public RelyingPartyRegistration findByRegistrationId(String registrationId) {
        return configRepository.findByTenantId(registrationId)
                .map(this::toRegistration)
                .orElse(null);
    }

    private RelyingPartyRegistration toRegistration(TenantSamlConfig config) {
        List<Saml2X509Credential> verificationCredentials = config.getCertificates().stream()
                .filter(IdpCertificate::isActive)
                .map(cert -> Saml2X509Credential.verification(parseX509(cert.getCertificatePem())))
                .toList();

        return RelyingPartyRegistration
                .withRegistrationId(config.getTenantId())
                .entityId(config.getIssuerEntityId())
                .assertionConsumerServiceLocation("{baseUrl}/login/saml2/sso/{registrationId}")
                .assertionConsumerServiceBinding(Saml2MessageBinding.POST)
                .assertingPartyMetadata(party -> party
                    // IdP entity ID — in production store separately; using SSO URL as fallback
                    .entityId(config.getSingleSignOnServiceUrl())
                    .singleSignOnServiceLocation(config.getSingleSignOnServiceUrl())
                    .singleSignOnServiceBinding(Saml2MessageBinding.REDIRECT)
                    .verificationX509Credentials(creds -> creds.addAll(verificationCredentials))
                )
                .build();
    }

    private X509Certificate parseX509(String pem) {
        try {
            String cleaned = pem
                .replace("-----BEGIN CERTIFICATE-----", "")
                .replace("-----END CERTIFICATE-----", "")
                .replaceAll("\\s", "");
            byte[] decoded = Base64.getDecoder().decode(cleaned);
            CertificateFactory cf = CertificateFactory.getInstance("X.509");
            return (X509Certificate) cf.generateCertificate(new ByteArrayInputStream(decoded));
        } catch (Exception e) {
            throw new IllegalStateException("Failed to parse IdP certificate for tenant", e);
        }
    }
}