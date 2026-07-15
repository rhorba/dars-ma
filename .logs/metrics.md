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
