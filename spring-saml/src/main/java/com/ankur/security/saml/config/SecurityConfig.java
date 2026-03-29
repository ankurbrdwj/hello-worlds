package com.ankur.security.saml.config;

import com.ankur.security.saml.saml.DynamicRelyingPartyRegistrationRepository;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
public class SecurityConfig {

    /**
     * SAML2 filter chain — protects /saml/** routes.
     *
     * Spring Security handles for free:
     * - SP-initiated flow (AuthnRequest build + redirect)
     * - IdP-initiated flow (no prior AuthnRequest)
     * - XML signature verification
     * - RelayState management
     * - SLO logout
     * - SP metadata endpoint at /saml2/service-provider-metadata/{registrationId}
     *
     * KAYAK provides: DynamicRelyingPartyRegistrationRepository (per-tenant DB lookup).
     */
    @Bean
    public SecurityFilterChain samlFilterChain(
            HttpSecurity http,
            DynamicRelyingPartyRegistrationRepository registrationRepository) throws Exception {

        http
            .securityMatcher("/saml/**", "/login/saml2/**", "/logout/saml2/**")
            .authorizeHttpRequests(auth -> auth
                .anyRequest().authenticated()
            )
            .saml2Login(saml -> saml
                .relyingPartyRegistrationRepository(registrationRepository)
            )
            .saml2Logout(saml -> saml
                .relyingPartyRegistrationRepository(registrationRepository)
            );

        return http.build();
    }

    /**
     * Admin API filter chain — protects /api/** config endpoints.
     *
     * In production: restrict to internal platform admin roles.
     * Here: permit all for local development.
     */
    @Bean
    public SecurityFilterChain adminApiFilterChain(HttpSecurity http) throws Exception {
        http
            .securityMatcher("/api/**")
            .authorizeHttpRequests(auth -> auth
                .anyRequest().permitAll()
            )
            .csrf(csrf -> csrf.disable()); // REST API — CSRF not needed

        return http.build();
    }
}