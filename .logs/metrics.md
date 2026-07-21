# METRICS — Dars.ma



## SPRINT_SNAPSHOT — Sprint 1 (Foundation & Auth) — 2026-07-05
Stories completed: 1.1 (scaffold), 1.2 (migrations), 1.3 (auth), 1.4 (Angular auth shell)
Backend coverage: 84% instructions (combined unit+integration via JaCoCo, gate >=80% MET)
Frontend coverage: 93.46% lines (Vitest, gate >=80% MET)
CI: not yet exercised on this push (first push of the sprint) - will monitor per DevOps CI Monitoring Protocol after push
Security: Gitleaks clean. Trivy: attempted, inconclusive locally due to environment friction, enforced in CI going forward.

## SPRINT_SNAPSHOT — Sprint 2 (Profiles & Verification) — 2026-07-06
Stories completed: 2.1 (tutor profile CRUD), 2.2 (verification document upload, AES-256-GCM), 2.3 (admin approve/reject queue), 2.4 (VerificationGuard)
Backend coverage: 91% instructions (gate >=80% MET)
Frontend coverage: 88.6% statements / 94.2% lines (gate >=80% MET)
CI: green on first attempt (run 28797374911)
Security: relied on CI's gitleaks-action (no local binary available this session)

## SPRINT_SNAPSHOT — Sprint 3 (Gigs & Matching) — 2026-07-15
Stories completed: 3.1 (gig request creation), 3.2 (multilingual embedding generation), 3.3 (pgvector match suggestions + thin-pool fallback), 3.4 (match list UI + tutor browse/filter fallback)
Backend coverage: 90.44% instructions / 79.41% branches (JaCoCo, gate >=80% instructions MET; branch coverage is not the gating metric per established convention but tracked here for visibility)
Frontend coverage: 87.31% statements / 92.71% lines / 89.51% branches / 77.86% functions (Vitest v8, gate >=80% statements/lines MET)
CI: to be monitored after this session's push per DevOps CI Monitoring Protocol
Security: no local gitleaks binary available (consistent with sessions 3-4); manual diff review found no secrets; all new SQL is parameterized. Full scan deferred to CI.

## SPRINT_SNAPSHOT — Sprint 4 (Booking & Escrow, batches 1-3: stories 4.1-4.3) — 2026-07-15
Stories completed: 4.1 (EscrowPaymentProvider + Mock), 4.2 (create booking + escrow hold), 4.3 (mutual completion + escrow release). Story 4.4 (dispute) deferred to Sprint 5.
Backend coverage: 92.17% instructions / 83.67% branches (gate >=80% MET)
Frontend coverage: 87.04% statements / 92.61% lines / 89.61% branches / 78.37% functions (gate >=80% statements/lines MET)
Test rigor: Maximum (per Test Strategy risk assessment) - full adversarial checklist covered including a real concurrent-completion race test (two threads + DB pessimistic lock)
CI: to be monitored after this session's push
Security: no local gitleaks binary; manual review found no secrets, all SQL parameterized

## SPRINT_SNAPSHOT — Sprint 4 (Booking & Escrow, complete: stories 4.1-4.4) — 2026-07-15
Story 4.4 (dispute flow, admin-mediated) shipped, closing out Epic 4 in full.
Backend coverage: 92.56% instructions / 86.36% branches (gate >=80% MET)
Frontend coverage: 86.12% statements / 92.24% lines / 89.83% branches / 78.31% functions (gate >=80% statements/lines MET)
Test rigor: Maximum - adversarial checklist fully covered across all 4 stories; the earlier session's flaky frontend test (tutor-profile-form timeout under load) did not recur on this run
CI: to be monitored after this session's push
Security: no local gitleaks binary; manual review found no secrets; dispute resolution reuses the existing ADMIN-role matcher, no new security surface

## SPRINT_SNAPSHOT — Sprint 8 (Epic 7: i18n/RTL Polish, Hardening & Launch) — 2026-07-21
Stories completed: 7.1 (i18n/RTL full pass + critical TranslateHttpLoader shadowing fix), 7.2 (upload content-sniffing + JWT expired/tampered/missing-token coverage), 7.4 (E2E critical-path suite FR+AR + docker-compose.prod.yml + video recording). Story 7.3 (CMI live switch) skipped again - no real sandbox credentials.
Backend coverage: 91% instructions / 82% branches (JaCoCo, full `mvn verify` incl. Testcontainers ITs, gate ≥80% MET)
Frontend coverage: 86.83% statements / 91.01% branches / 79.53% functions / 92.24% lines (Vitest v8, 107 tests/28 files, gate ≥80% statements/lines MET)
E2E: Playwright critical-path suite (registration → verification → admin approval → gig → booking → escrow → completion → review), parameterized FR+AR, both green. AR run asserts RTL. Video recorded to `.recordings/v1-2026-07-21-fr.webm` and `.recordings/v1-2026-07-21-ar.webm` (first project-version-completion milestone, rule 9).
Security: `npm audit --omit=dev --audit-level=high` → 0 vulnerabilities. No local gitleaks binary (consistent since session 3); manual review of all changed files found no secrets. Full scan deferred to CI's gitleaks-action.
Release Gate Criteria (Test Strategy §5): all 5 criteria met - see docs/test-strategy-dars-ma.md.
Notable: this sprint's first real (non-mocked) end-to-end run surfaced 7 real bugs total across 2 sessions that had been invisible to every unit/integration test up to this point - see .logs/issues.md for full detail on each.
