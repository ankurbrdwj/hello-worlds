# KAYAK for Business — Project Context

## Product Overview

**Corporate Travel Powered by KAYAK**

KAYAK for Business brings KAYAK's intuitive, consumer-grade user experience to the corporate world, enabling companies to seamlessly book, manage, and track corporate travel in one place. Powers modern travel programs for startups, global brands, and everything in between.

Built in collaboration with **Block Sky** (Blockskye). Core philosophy: build the platform around the needs of the **travelers themselves**, not just the organization.

---

## Business Use Cases (Product)

### UC-1: Branded Fares in Search Results
**Who:** Traveler + Travel Admin
**What:** Search results surface branded fare tiers with status-based perks (priority boarding, seat selection, baggage) alongside pricing.
**Why it matters:**
- Traveler sees full value of each fare option (not just price)
- Choices are pre-filtered to stay within company travel policy
- Reduces back-and-forth between booking and compliance checks
- Frequent flyers can leverage their status perks without leaving the corporate tool

### UC-2: Expense Module — Policy Validation at Checkout
**Who:** Traveler + Finance Team
**What:** Travel eligibility and policy compliance are validated inline during checkout — before booking is finalized.
**Why it matters:**
- Eliminates traditional post-trip expense report cycle
- Finance team avoids chasing receipts or reconciling discrepancies
- Policy violations caught before spend happens, not after
- Blockskye settlement layer eliminates the credit card entirely

### UC-3: Multi-Channel Trip Changes & Customer Service
**Who:** Traveler
**What:** Trip modifications accessible via three channels:
1. Directly on KAYAK platform (self-serve)
2. Via Block Sky agent (assisted)
3. Directly with the travel supplier (airline/hotel)

---

## Key Design Principles

| Principle | Description |
|---|---|
| User-Centered | Built around traveler needs, not admin/finance convenience |
| Shift-Left Compliance | Policy validated at booking time, not post-trip |
| Automation First | Expense reporting automated via Blockskye settlement |
| Flexible Support | Multiple channels for resolution |
| Duty of Care | Org-level safety and compliance met without UX compromise |

---

## GDS / Distribution Context

**GDS = Global Distribution System** — the middleware between airlines/hotels (suppliers) and travel agents/booking tools (buyers). The three dominant players are Sabre, Amadeus, and Travelport.

**Amadeus business model:** Transaction fee per booking (~€48/booking). Revenue €6.14B in 2024. Also sells IT solutions to airlines (Altéa PSS).

**Blockskye disruption angle:** Blockchain single-record-of-truth architecture has GDS-like functions but eliminates the intermediary. The ARC proof-of-concept with United Airlines validated blockchain-based ticket settlement. KAYAK likely queries GDS content (including Amadeus NDC) while Blockskye handles settlement.

**NDC (New Distribution Capability):** IATA standard allowing airlines to distribute rich content directly, bypassing traditional GDS fare structures. Amadeus has 70+ NDC agreements. KAYAK for Business likely uses both GDS + NDC content.

---

## System Design: Search Aggregation (Fan-out / Fan-in)

**Pattern:** Scatter-gather. One search request fans out to 200+ partner APIs, results fan back in.

**Key design decisions:**
- `CompletableFuture.allOf()` with per-partner 1.8s timeout
- `exceptionally(ex -> Collections.emptyList())` — silence = empty list, NOT failure
- Resilience4j `CircuitBreaker` per partner — opens after 5 failures in 10s
- Redis cache keyed by `{userId, origin, destination, dates, policyTier}` — TTL 60s
- Policy tier is part of cache key — different tier = different cached results
- Result aggregator runs on timeout (not completion) — partial results are first-class
- Policy filter + ranker applied AFTER aggregation — strips non-compliant fares

---

## System Design: Policy Validation at Checkout

**Key design decisions:**
- Policy loaded synchronously at checkout start (Redis cache, 5-10min TTL)
- Rules engine evaluates budget / fare class / hotel tier IN PARALLEL via CompletableFuture
- Violations classified: **soft** (flag + notify) vs **hard** (block booking)
- Idempotency key = hash(userId + flightId + date) stored in Redis — prevents double booking
- Audit event fires on ALL paths (booked, soft violation, hard violation)
- Audit event captures `policyVersion` at decision time — critical for compliance audit months later
- Append-only audit log — never mutate audit records

---

## SAML Architecture — This Repo (spring-saml)

### What Spring Security gives for FREE
```
saml2Login()           → builds AuthnRequest, signs, redirects to IdP
                       → receives POST back, verifies XML signature
                       → parses all assertion attributes
                       → handles IdP-initiated flow (no prior AuthnRequest)
                       → manages RelayState
                       → clock skew on NotBefore/NotOnOrAfter
saml2Logout()          → full SLO flow, session invalidation
verificationX509Credentials → multi-cert support (old + new simultaneously)
signingX509Credentials → SP signing of AuthnRequests
decryptionX509Credentials → EncryptedAssertion decryption
RelyingPartyRegistrations.fromMetadataLocation(url) → parse IdP metadata in one line
/saml2/service-provider-metadata/{id} → SP metadata endpoint auto-exposed
```

### What KAYAK builds on top (business layer)

**UC-S01: Per-tenant RelyingPartyRegistration** — DB-backed repository, one registration per enterprise customer. Spring provides the interface, KAYAK implements the DB-backed version.

**UC-S02: RelayState 80-byte truncation** — custom `Saml2AuthenticationRequestResolver` for legacy IdP compatibility.

**UC-S03: Custom user identifier extraction** — Spring uses NameID by default. Switching to `employeeId` or `ObjectGUID` requires custom `OpenSaml4AuthenticationProvider` with custom principal extractor.

**UC-S04: Per-tenant attribute mapping** — translate IdP claim names to internal fields. Azure sends long URI format (`http://schemas.xmlsoap.org/ws/2005/05/identity/claims/emailaddress`), Okta/Google send short names (`email`). Spring delivers raw attributes, translation dict is KAYAK's code:
```java
Map<String, String> azureMapping = Map.of(
    "http://schemas.xmlsoap.org/ws/2005/05/identity/claims/emailaddress", "email",
    "http://schemas.xmlsoap.org/ws/2005/05/identity/claims/givenname", "displayName"
);
```

**UC-S05: Just-in-time provisioning** — `AuthenticationSuccessHandler` creates user record on first login from assertion attributes. Spring authenticates but never persists to your DB.

**UC-S06: Admin cert CRUD API** — REST endpoints to upload, validate (`CertificateFactory`), and remove IdP public key certificates. Spring has no cert management UI.

**UC-S07: Auto cert rotation** — `@Scheduled` job polls federation metadata URL hourly (Azure supports this, Okta/Google require manual rotation). Hot-reload of `RelyingPartyRegistrationRepository` without restart.

**UC-S08: Admin-controlled session TTL** — `OncePerRequestFilter` reads tenant policy and calls `session.setMaxInactiveInterval()`. Spring Session provides plumbing, per-tenant logic is KAYAK's.

**UC-S09: ForceAuthn** — custom `Saml2AuthenticationRequestResolver` sets `ForceAuthn=true` (~5 lines).

**UC-S10: Attribute-based access rules** — restrict login by group/assertion value. Spring handles path-based rules, custom `AuthorizationManager` or `GrantedAuthoritiesMapper` needed for attribute rules.

**UC-S11: UC-24 — Attribute sync on every login** — THE most important one:
```java
// Spring delivers fresh attributes on every login for free
String newPolicyTier = principal.getFirstAttribute("travelPolicyTier");

// KAYAK builds: diff, DB write, cache invalidation
if (!Objects.equals(stored.getPolicyTier(), newPolicyTier)) {
    stored.setPolicyTier(newPolicyTier);
    userRepo.save(stored);
    policyCache.evict(stored.getId()); // CRITICAL — stale cache = wrong fares
    auditService.logAttributeChange(stored.getId(), p);
}
```
If cache not invalidated after policy tier change, promoted employee still sees economy-only results until TTL expires.

**UC-S12: Profile image import** — download from URL assertion or decode Base64 assertion on user creation. Spring's WebClient downloads it, S3/DB storage is KAYAK's.

**UC-S13: External employee ID mapping** — store company `employeeId` alongside internal userId for reporting/audit.

**UC-S14: SAML debug endpoint** — `/saml-login` troubleshoot page (admin only via Spring Security). Page content — decoded assertion, auth log — is custom.

**UC-S15: SAML response DB logging** — custom `Saml2AuthenticationProvider` wrapper that persists raw XML. `@ConditionalOnProperty` toggle.

**UC-S16: Config test endpoint** — dry-run full auth flow before applying settings live. Custom orchestration — no Spring equivalent.

**UC-S17: UTF-8 BOM on EntityDescriptor.xml** — small filter override for legacy IdP compatibility.

**UC-S18: SubjectConfirmation toggle** — custom `OpenSaml4AuthenticationProvider` with modified response validator (some IdPs don't send it).

**UC-S19: Disable SP-initiated / IdP-only mode** — custom resolver that redirects to IdP URL instead of generating AuthnRequest.

**UC-S20: SCIM provisioning endpoints** — no Spring support at all. Custom `@RestController` for user create/update/deactivate, group sync. Deprovisioning edge cases: active bookings, mid-trip, re-hire.

---

## Spring Coverage Summary

| Verdict | Count | UCs |
|---|---|---|
| Spring 100% (zero code) | 7 | UC-S: SP metadata, IdP metadata parse, SP-initiated flow, IdP-initiated flow, signature verify, SLO logout, multi-cert support |
| Config only | 4 | Protocol binding, assertion decryption, SP signing, ForceAuthn toggle |
| Spring partial (you finish) | 10 | Per-tenant repo, RelayState, SubjectConfirmation, user ID extraction, cert rotation, session TTL, access rules, disable SP-init, attribute sync, external ID |
| Build entirely | 8 | Attribute mapping, JIT provisioning, cert CRUD API, profile image, debug page, response logging, config test, SCIM |

---

## IdP Comparison (Okta vs Azure Entra ID vs Google)

| Dimension | Okta | Azure Entra ID | Google Workspace |
|---|---|---|---|
| App catalog | Pre-built (search "Stack Internal") | Enterprise gallery or custom (non-gallery) | No catalog — always custom SAML |
| Cert renewal | Manual paste | Auto via Federation Metadata URL | Manual rotation |
| Attribute format | Short names: `email`, `displayName` | Long URI: `…/claims/emailaddress` | Short names: `email`, `displayName` |
| Default user ID | Okta username (stable) | Subject/NameID = userprincipalname | Primary email (risky if email changes) |
| Full name field | Concatenate `${firstName} ${lastName}` | givenname claim (first name only) | No full name — needs custom attribute |
| IdP-initiated | Tile on Okta dashboard | Tile on MyApps portal | Tile in Google app launcher |

**Critical:** Azure's long URI claim names break attribute extractors that expect short names. Must detect IdP type per tenant and apply correct mapping dict.

---

## Session Architecture — Dual Session Isolation

**Problem:** One Spring app, two session types (public browsing + SSO-gated Teams). They must not collide.

**Solution:**
1. Two `SecurityFilterChain` beans — `@Order(1)` for `/teams/**` (SAML2), `@Order(2)` for `/**` (public)
2. Cookie path scoping — `SESSION` cookie `Path=/`, `TEAMS_SESSION` cookie `Path=/teams`
3. Browser enforces isolation — `/teams/**` sends both cookies, public paths only send `SESSION`
4. Each chain reads its own cookie — invalidating Teams session leaves public session untouched

**Session lifetimes:**
- Public session: 8 hours, idle expiry, no force re-auth
- Teams session: admin-controlled TTL (e.g. 30 min), forced re-auth on expiry

**Mid-checkout protection:** Filter extends Teams session automatically if active booking in progress — never prompt user mid-checkout.

---

## TLS vs SAML Signing — Two Different Certificates

| | TLS Certificate | SAML Signing Certificate |
|---|---|---|
| Issued by | Public CA (Let's Encrypt, DigiCert) | IdP self-issued (Okta auto-generates) |
| Purpose | Encrypt the wire | Sign the assertion payload |
| Verified by | Browser CA trust store | SP's hardcoded copy of IdP cert |
| Expires | 90 days – 2 years | Often 10 years, manually rotated |
| Needed for | Every HTTPS connection | Every SAML assertion |

**Why both needed:** TLS secures the channel but terminates at load balancers. SAML message signing protects the payload regardless of how many intermediaries it passes through.

---

## JKS (Java KeyStore)

JKS = Java's native password-protected binary keystore. Holds three entry types by alias:
- `PrivateKeyEntry` — your private key + cert chain (sign outbound AuthnRequests)
- `TrustedCertEntry` — IdP public cert (verify inbound assertion signatures)
- `SecretKeyEntry` — symmetric key (for assertion encryption, rare)

**Key commands:**
```bash
# Generate SP keypair
keytool -genkeypair -alias sp-signing -keyalg RSA -keysize 2048 \
  -validity 3650 -keystore keystore.jks -storepass changeit

# Export SP public cert to share with IdP
keytool -exportcert -alias sp-signing -keystore keystore.jks \
  -storepass changeit -file sp-public.cer -rfc

# Import IdP cert as trusted entry
keytool -importcert -alias okta-idp -file okta.cer \
  -keystore keystore.jks -storepass changeit

# List all entries
keytool -list -v -keystore keystore.jks -storepass changeit
```

**JKS vs PKCS12:** JKS is Java-proprietary. Since Java 9 the default is PKCS12 (`.p12`). Spring Boot prefers PKCS12. Convert: `keytool -importkeystore -srckeystore keystore.jks -destkeystore keystore.p12 -deststoretype PKCS12`

---

## IdP Session Independence

Once the SAML assertion is consumed by KAYAK, **IdP and KAYAK have zero ongoing connection**.

```
T=0       User hits KAYAK → redirect to bank IdP
T=1s      IdP sends SAML assertion → KAYAK creates SP session → IdP conversation OVER
T=15min   Bank IdP session expires → KAYAK doesn't know, doesn't care
T=8h      KAYAK SP session expires → redirect to IdP for fresh assertion
          If IdP session alive → silent re-auth
          If IdP session also expired → user sees bank login screen
```

**Exception:** `ForceAuthn=true` forces the IdP to re-authenticate regardless of existing IdP session. Used when bank policy requires physical password entry every login.

---

## Stack Overflow vs KAYAK Build Comparison

**Stack Overflow (3 engineers, 3 months):** Built on .NET, no SAML library. Had to build: credential creation from assertions, SSO session management, dual-session isolation — everything from scratch.

**KAYAK (Spring Boot):** Spring Security `saml2-service-provider` handles all protocol mechanics. KAYAK builds: per-tenant registration, attribute mapping, JIT provisioning, SCIM endpoints, session TTL filter, cert rotation scheduler, debug tooling.

---

## Interview Prep — KAYAK Mid-Level Java Backend (Berlin)

### Role Summary
K4B mid-level Java backend. 200+ travel partner integrations, 500K hotels, 550 airlines. Core focus: search/booking performance, third-party API integrations, B2B identity (SAML/SCIM/SSO). Berlin office, 3 days/week. Interviewers: Giedrius Liutkus (Software Architect) + Hlib Babii (Senior SWE). 2 hours, no fixed syllabus.

### Opening (rehearse under 3 minutes)
> "I'm a backend engineer with 10+ years, currently at Deutsche Bank in Berlin managing 45 microservices handling billions of daily computations. My core stack is Java and Spring Boot — distributed systems, resilience patterns, async processing at scale.
> What drew me to K4B specifically is the integration surface — 200+ travel partners, policy-aware search, and the Blockskye settlement model that eliminates expense reports at the architecture level. That's a genuinely interesting distributed systems problem.
> On the identity side, I've been hands-on with SAML and Spring Security recently — that's what brought me to this project."

### Round Guide

**Round 1 — Fit/motivation:** Why KAYAK, why K4B, career narrative (seeking product impact, not leaving DB).

**Round 2 — Java/Spring (Hlib leads):**
- `@Transactional` propagation: `REQUIRED` vs `REQUIRES_NEW` — audit logs use `REQUIRES_NEW` so they persist even if outer tx rolls back
- `CompletableFuture` fan-out pattern for 200 partner APIs
- Circuit breaker (Resilience4j) per partner
- Redis vs PostgreSQL for search cache
- Flyway/Liquibase zero-downtime migration pattern

**Round 3 — System Design (Giedrius leads):**
- Search aggregation fan-out: scatter-gather, 1.8s timeout, partial results, policy filter
- Policy validation at checkout: parallel rules, soft/hard violations, idempotency, append-only audit
- SAML/SSO onboarding: SP-initiated flow, per-tenant attribute mapping, SCIM

**Round 4 — SAML/Identity deep-dive:**
- SP-initiated flow end to end
- What Spring gives free vs what KAYAK builds
- Azure URI claim names vs Okta short names — attribute mapping layer
- UC-24: attribute sync on every login — diff + DB write + cache invalidation
- JKS: PrivateKeyEntry for signing, TrustedCertEntry for IdP cert verification
- SCIM deprovisioning edge cases: active bookings, mid-trip, re-hire

**Round 5 — Behavioral:**
- Full ownership: any DB migration or microservice you led at Deutsche Bank
- Production incident: detection, mitigation, root cause, permanent fix
- Code review: quality standards differ
- Pushback on design decision

### Questions to ask them
- "How does K4B currently handle the fan-out to 200+ partners — GDS abstraction layer or direct NDC connections?"
- "What does full ownership look like — do engineers ship to prod directly or is there a release gate?"
- "Where is the biggest current pain in the integration surface — reliability, data normalisation, or latency?"
- "How does the Blockskye settlement layer interact with the Spring services — event-driven or synchronous?"

### Key Differentiators
| Their Need | Your Signal |
|---|---|
| High-scale Java backend | 45 microservices, billions of daily computations at Deutsche Bank |
| Travel domain / GDS knowledge | Researched KAYAK × Blockskye partnership, GDS model, NDC, Amadeus |
| SAML/SCIM/SSO | This repo — hands-on Spring Security SAML2 implementation |
| Full ownership | Led architecture decisions with minimal guidance at Deutsche Bank |
| B2B integrations | Financial system integrations map directly to travel partner API patterns |

### Tonight's prep (3 things only)
1. Say the fan-out design out loud — "silence is an empty list, not a failure"
2. Re-read this file — domain vocabulary must flow naturally
3. `@Transactional` propagation — write two sentences on REQUIRED vs REQUIRES_NEW