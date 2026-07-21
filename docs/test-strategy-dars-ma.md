# Test Strategy: Dars.ma
**PRD Reference**: docs/prd-dars-ma.md
**Architecture Reference**: docs/architecture-dars-ma.md
**Version**: 1.0 | **Date**: 2026-07-04 | **Author**: Test Architect

## 1. Risk Assessment
| Component | Impact | Frequency | Complexity | Risk | Test Level |
|---|---|---|---|---|---|
| Auth (JWT) | Critical (5) | Low (2) | Medium (3) | 10 | High |
| Booking/Escrow state machine | Critical (5) | Medium (3) | High (5) | 13 | Maximum |
| Verification workflow (admin approve/reject) | High (4) | Low (2) | Low (2) | 8 | Standard |
| pgvector matching | Medium (3) | Medium (3) | High (5) | 11 | High |
| Messaging | Medium (3) | Low (2) | Low (2) | 7 | Standard |
| i18n / RTL rendering | Medium (3) | Medium (3) | Medium (3) | 9 | High |
| Admin analytics dashboard | Low (2) | Low (2) | Low (1) | 5 | Standard |
| Notifications | Low (2) | Low (2) | Low (2) | 6 | Standard |

## 2. Test Pyramid Targets
| Layer | Coverage Target | Tooling |
|---|---|---|
| Unit | ≥ 60% of business logic | JUnit 5 + Mockito (backend), Jasmine/Karma or Jest (Angular) |
| Integration | ≥ 40% of API + DB layer | Spring Boot Test + Testcontainers (real Postgres+pgvector in CI) |
| E2E | Critical happy paths only | Playwright (booking flow, verification flow, i18n switch) |
| **Combined gate** | **≥ 80%** — non-negotiable | CI blocks merge if below (see DevOps doc) |

## 3. ATDD Acceptance Scenarios (critical paths)

```gherkin
Feature: Escrow-backed booking

  Scenario: Student books a matched tutor and escrow is held
    Given Yasmine is logged in as a student with an open gig request
    And Karim is a VERIFIED tutor suggested as a match
    When Yasmine confirms a booking with Karim and completes CMI payment
    Then the booking status should be ESCROW_HELD
    And an escrow_transaction record should exist with status HELD

  Scenario: Escrow releases only after both parties confirm completion
    Given a booking with status ESCROW_HELD
    When only the student marks the session complete
    Then the booking status should remain ESCROW_HELD
    When the tutor also marks the session complete
    Then the booking status should become COMPLETED
    And the escrow_transaction status should become RELEASED

  Scenario: Payment failure does not create a false escrow hold
    Given Yasmine attempts to book Karim
    When the CMI payment fails
    Then the booking status should remain PENDING_PAYMENT
    And no escrow_transaction should be created

Feature: Tutor verification

  Scenario: Unverified tutor cannot accept bookings
    Given Karim's verification_status is PENDING
    When Karim attempts to accept a match
    Then the request should be rejected with "Verification required"

  Scenario: Admin approves a tutor
    Given a verification_document submitted by Karim with status PENDING
    When an admin approves it
    Then Karim's verification_status should become VERIFIED
    And Karim should receive a notification
    And an audit_log_entry should record the approval with the admin's id

Feature: Trilingual + RTL

  Scenario: Arabic renders right-to-left
    Given a user with preferred_lang = 'ar'
    When they load any authenticated page
    Then the document direction should be "rtl"
    And the booking stepper and message bubbles should mirror layout correctly
```

## 4. Adversarial Checklist (high-risk components only)

### Booking/Escrow (Maximum rigor)
- [ ] Client sends a manipulated `agreed_price_mad` on booking creation → server must recompute from the gig/tutor rate, never trust client body
- [ ] Double-submit "Mark complete" or "Create booking" → idempotency check on booking id / state transition guard
- [ ] Race: both parties confirm completion simultaneously → DB-level transaction ensures exactly-once transition to COMPLETED
- [ ] CMI webhook replay (same payment confirmation sent twice) → dedupe by `cmi_reference`
- [ ] Booking accessed by a user who isn't the student or tutor on it (IDOR) → 403
- [ ] Escrow release attempted via direct API call bypassing both-confirmation rule → server enforces state machine, not client

### Auth
- [ ] Expired/tampered JWT → rejected with 401, no partial trust
- [ ] Refresh token reuse after rotation → revoked, all sessions invalidated
- [ ] Privilege escalation: STUDENT role token calling `/admin/*` endpoints → 403 via `@PreAuthorize`

### Verification uploads
- [ ] Oversized file, wrong MIME type (e.g., executable renamed .pdf) → rejected server-side by content sniffing, not just extension
- [ ] Path traversal in filename → filename never used as a path component

### Matching (pgvector)
- [ ] Gig with no candidate tutors → graceful empty state (see UX), not a 500
- [ ] Extremely long gig description (10K+ chars) → truncated/validated before embedding generation

## 5. Release Gate Criteria
- [x] All ATDD acceptance scenarios pass (Playwright critical-path spec: register → tutor verification → admin approval → gig creation → booking → escrow → mutual completion → review, FR+AR, 2026-07-21)
- [x] Combined unit + integration coverage ≥ 80% (backend 91%/82% instructions/branches, frontend 86.83%/92.24% statements/lines, 2026-07-21 — CI-enforced, see DevOps doc)
- [x] No critical/high security findings open (Security Baseline + DevSecOps scans; `npm audit --omit=dev` 0 vulnerabilities; no local gitleaks binary, relying on CI's gitleaks-action per established convention)
- [x] E2E happy paths pass (booking flow, verification flow, i18n switch) — video recorded this sprint (v1, final sprint per project convention): `.recordings/v1-2026-07-21-fr.webm`, `.recordings/v1-2026-07-21-ar.webm`
- [x] RTL smoke-tested on every screen that shipped that sprint (AR Playwright run asserts `dir="rtl"`; manual browser verification in Batch 1 covered every route category)
