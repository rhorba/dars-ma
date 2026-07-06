# SESSIONS — Dars.ma



## SESSION_START — 2026-07-04
Resumption check: logs empty, no prior session. Fresh start.
Request: pivot stack to Java + Angular (latest LTS), full sprint backlog, strict skill/phase adherence, push every sprint, video recording deferred to last sprint only, Docker-only deploy (K8s only if needed).
Note: README.md documents a PRIOR stack decision (Next.js/TS/Postgres/Drizzle) — conflicts with new request, flagged to user for resolution before BRAINSTORM.

## SESSION_END — 2026-07-04
Completed: All 10 mandatory foundation docs (PRD, System Design, Architecture, Security, Database, UX, UI, Test Strategy, DevOps, Stories/8-sprint backlog) drafted and approved. Stack pivot to Java 25/Spring Boot/Maven + Angular LTS + PostgreSQL/pgvector + Docker-only confirmed and applied to README. Git initialized, remote set, foundation commit pushed to github.com/rhorba/dars-ma (main).
Next: Session 2 begins Sprint 1 execution (Story 1.1 scaffold: Spring Boot + Angular + Docker Compose skeleton, then 1.2-1.4). No code written yet this session per rule 13.
Decisions to remember: CMI_MODE=mock until real sandbox creds provided (Story 7.3 revisits); Kubernetes explicitly deferred (Docker Compose only); video recording deferred to Sprint 8 only (Story 7.4).

## SESSION_START — 2026-07-04 (session 2)
Resuming from session 1 SESSION_END. Starting Sprint 1 execution (Foundation & Auth: stories 1.1-1.4).

## SESSION_END — 2026-07-06
Sprint 1 (Foundation & Auth) fully shipped: backend scaffold, Flyway migrations, JWT auth (register/login/refresh with rotation+reuse-detection), Angular auth shell with FR/AR(RTL)/EN i18n, Docker Compose stack, CI pipeline. Coverage gates met (backend 84%, frontend 93.46%). CI green on github.com/rhorba/dars-ma main (commit d023581 and its predecessors). No code before docs was violated - foundation docs (session 1) shipped before any code, per rule 13.
Next: Sprint 2 (Profiles & Verification) - stories 2.1-2.4 per docs/stories-dars-ma.md.
Known follow-ups: Trivy's Java analyzer needs a warm Maven cache to avoid rate-limiting (documented in CI); CMI escrow still in mock mode (real credentials not yet available); video recording intentionally deferred to Sprint 8 per project convention.

## SESSION_END — 2026-07-06 (session 3)
Sprint 2 (Profiles & Verification) fully shipped: tutor profile CRUD + public view, encrypted (AES-256-GCM) verification document upload, admin approve/reject queue with in-app notifications + audit log, VerificationGuard unit-tested standalone for Epic 4 reuse. Pulled notifications/audit_log_entries tables forward from the original Sprint 7 migration plan (V005); added mime_type/original_filename to verification_documents (V006) so admins can actually review uploaded documents. Coverage gates met (backend 91% instructions, frontend 88.6% statements/94.2% lines). CI green on first attempt (run 28797374911, commit d16a631).
Next: Sprint 3 (Gigs & Matching) - stories 3.1-3.4 per docs/stories-dars-ma.md.
Known follow-ups: same as Sprint 1 (Trivy Maven cache warm-up, CMI mock mode, video recording deferred to Sprint 8); no local gitleaks binary available this session, relied on CI's gitleaks-action for the final secret-scan pass.
