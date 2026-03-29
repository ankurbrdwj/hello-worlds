package com.ankur.security.saml.service;

import com.ankur.security.saml.model.IdpCertificate;
import org.springframework.stereotype.Service;

import java.io.ByteArrayInputStream;
import java.security.MessageDigest;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.util.Base64;
import java.util.HexFormat;

/**
 * Parses and validates IdP X.509 certificates.
 * UC-S06: used by the cert CRUD API to validate uploaded certs before storing.
 */
@Service
public class CertificateService {

    /**
     * Parse the PEM certificate and populate metadata fields on the IdpCertificate.
     * Throws IllegalArgumentException if the PEM is invalid.
     */
    public IdpCertificate parseAndValidate(IdpCertificate cert) {
        X509Certificate x509 = parsePem(cert.getCertificatePem());

        cert.setIssuer(x509.getIssuerX500Principal().getName());
        cert.setSubject(x509.getSubjectX500Principal().getName());
        cert.setValidFrom(x509.getNotBefore().toInstant());
        cert.setValidTo(x509.getNotAfter().toInstant());
        cert.setSignatureAlgorithm(x509.getSigAlgName());
        cert.setThumbprint(computeThumbprint(x509));

        return cert;
    }

    /**
     * Validate only — returns parsed metadata without modifying input.
     * Used by the /validate endpoint.
     */
    public CertificateValidationResult validate(String pem) {
        try {
            X509Certificate x509 = parsePem(pem);
            return CertificateValidationResult.valid(
                x509.getIssuerX500Principal().getName(),
                x509.getSubjectX500Principal().getName(),
                x509.getNotBefore().toInstant(),
                x509.getNotAfter().toInstant(),
                x509.getSigAlgName(),
                computeThumbprint(x509)
            );
        } catch (Exception e) {
            return CertificateValidationResult.invalid(e.getMessage());
        }
    }

    private X509Certificate parsePem(String pem) {
        try {
            String cleaned = pem
                .replace("-----BEGIN CERTIFICATE-----", "")
                .replace("-----END CERTIFICATE-----", "")
                .replaceAll("\\s", "");
            byte[] decoded = Base64.getDecoder().decode(cleaned);
            CertificateFactory cf = CertificateFactory.getInstance("X.509");
            return (X509Certificate) cf.generateCertificate(new ByteArrayInputStream(decoded));
        } catch (Exception e) {
            throw new IllegalArgumentException("Invalid certificate PEM: " + e.getMessage(), e);
        }
    }

    private String computeThumbprint(X509Certificate cert) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-1");
            byte[] digest = md.digest(cert.getEncoded());
            return HexFormat.ofDelimiter(":").formatHex(digest).toUpperCase();
        } catch (Exception e) {
            return "UNKNOWN";
        }
    }

    public record CertificateValidationResult(
        boolean valid,
        String error,
        String issuer,
        String subject,
        java.time.Instant validFrom,
        java.time.Instant validTo,
        String signatureAlgorithm,
        String thumbprint
    ) {
        static CertificateValidationResult valid(String issuer, String subject,
                java.time.Instant validFrom, java.time.Instant validTo,
                String sigAlg, String thumbprint) {
            return new CertificateValidationResult(true, null, issuer, subject, validFrom, validTo, sigAlg, thumbprint);
        }

        static CertificateValidationResult invalid(String error) {
            return new CertificateValidationResult(false, error, null, null, null, null, null, null);
        }
    }
}