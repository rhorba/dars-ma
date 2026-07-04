# Security Baseline: Dars.ma
**Architecture Reference**: docs/architecture-dars-ma.md
**Version**: 1.0 | **Date**: 2026-07-04 | **Author**: Security Engineer

## 1. Threat Model (5-Minute)
- **What are we building?** A marketplace that holds real money in escrow (CMI) and PII/identity documents (tutor verification) for students and tutors.
- **Who would attack it?** Opportunistic fraudsters (fake tutor verification, escrow abuse/chargebacks), competitors (data scraping), curious/malicious users (IDOR on other users' bookings/messages), not a nation-state-level target.
- **Worst outcome?** Financial loss via escrow manipulation, leak of identity documents (national ID/diplomas), account takeover leading to fraudulent bookings.

Given money + identity documents are in scope, this project sits at **"growing app"** tier, not solo-MVP: RBAC, rate limiting, encryption at rest, and audit logging are all justified — but no WAF/pen-testing/compliance program yet (no regulatory mandate identified, no enterprise customers).

## 2. STRIDE Analysis (top risks only)
| Threat | Component | Mitigation | Status |
|---|---|---|---|
| Spoofing | Auth (JWT) | Short-lived access tokens (15min) + refresh token rotation; RS256 signing | TODO |
| Tampering | Escrow state transitions | Server-side state machine (Booking status enum with valid-transition guard); no client-controlled amount/status fields | TODO |
| Repudiation | Booking completion / escrow release | Audit log table recording who/when/what for every escrow state change and verification decision | TODO |
| Info Disclosure | Verification documents, messages, bookings | Per-resource ownership checks (IDOR prevention); documents encrypted at rest; messages scoped to booking parties only | TODO |
| DoS | Public endpoints (register, login, gig browse) | Rate limiting on auth endpoints (Spring + bucket4j or similar) | TODO |
| Elevation of Privilege | Admin endpoints | Role check at controller level + method-level `@PreAuthorize`, not just UI hiding | TODO |

## 3. Authentication Strategy
- **Type**: JWT (access + refresh) — matches System Design's stateless/horizontally-scalable requirement (distributed-capable per decision tree)
- **MFA**: Not required for Student/Tutor at launch (YAGNI — no elevated risk per-user beyond account takeover, covered by strong passwords + rate limiting). **Required for Admin accounts** (TOTP) — admin can approve verifications and resolve escrow disputes, higher blast radius.
- **Password policy**: bcrypt/argon2id hashing, min 10 chars, checked against a common-password blocklist (not a live breach-API call — YAGNI for v1)
- **Session management**: Access token ≤ 15min, refresh token ≤ 7 days, refresh tokens stored HttpOnly+Secure+SameSite=Strict cookie; access token in memory (Angular), not localStorage (XSS mitigation)
- **Account lockout**: 5 failed attempts → exponential backoff (not permanent lock, to avoid DoS-via-lockout)

## 4. Authorization Model
- **Pattern**: Simple roles (Student / Tutor / Admin) + resource-ownership checks — RBAC is overkill for 3 roles with no sub-permissions; ownership checks handle the IDOR risk that pure role-checking misses
- **Roles defined**: `STUDENT`, `TUTOR`, `ADMIN`
- **Resource-level checks**: Yes, per-object — e.g., only the booking's student/tutor can view its messages/thread; only the profile owner can edit it or submit verification docs; only Admin can approve verification or resolve disputes

## 5. Data Protection
- **PII fields**: full name, email, phone, national ID / diploma images (verification documents), payment reference IDs (not full card data — CMI handles PCI scope)
- **Encryption at rest**: Verification documents stored encrypted (application-level encryption before write, or encrypted volume if stored as files outside Postgres); DB-level encryption for the verification-document and payment-reference columns
- **Encryption in transit**: HTTPS enforced everywhere (Nginx TLS termination), HSTS enabled
- **Secrets management**: All secrets (DB creds, JWT signing key, CMI API keys, SMTP creds) via environment variables, collected in `.env.example` (values never committed) — no secrets in code or logs

## 6. Security Requirements for Dev Team
- [ ] All inputs validated server-side (Bean Validation annotations on DTOs, never trust client)
- [ ] Output encoded for context (Angular's built-in sanitization for HTML; parameterized queries via JPA — no string-concatenated SQL)
- [ ] No secrets in code, logs, or error messages (error responses return generic messages, details logged server-side only)
- [ ] HTTPS only, security headers configured (HSTS, X-Content-Type-Options, X-Frame-Options, CSP for the Angular app)
- [ ] Dependencies scanned in CI (SCA — see DevOps doc for Trivy/OWASP Dependency-Check)
- [ ] File uploads (verification documents): type-checked (image/PDF only), size-limited, stored outside webroot, filename never trusted from client
- [ ] Escrow amount/status is always server-computed from the booking record — never accepted from client request body
