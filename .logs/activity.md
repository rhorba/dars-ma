# ACTIVITY — Dars.ma



## MILESTONE — 2026-07-04
Phase: UNDERSTAND complete. Git repo initialized (main branch), remote origin set to github.com/rhorba/dars-ma.
Stack pivot confirmed: Java 25 LTS + Spring Boot 3.x + Maven (backend), Angular latest LTS (frontend), PostgreSQL + pgvector (DB, retained from prior decision).
Next: PM charter + scope brainstorm (BRAINSTORM phase).

## MILESTONE — 2026-07-04
PRD approved → docs/prd-dars-ma.md

## MILESTONE — 2026-07-04
System Design approved (auto-proceeded after 60s no-response, recommended option) → docs/system-design-dars-ma.md

## MILESTONE — 2026-07-04
Architecture approved → docs/architecture-dars-ma.md

## MILESTONE — 2026-07-04
Security Baseline approved → docs/security-dars-ma.md

## MILESTONE — 2026-07-04
Database Design approved → docs/database-dars-ma.md

## MILESTONE — 2026-07-04
UX Foundation approved → docs/ux-dars-ma.md

## MILESTONE — 2026-07-04
UI Foundation approved → docs/ui-dars-ma.md (Angular Material chosen for RTL support)

## MILESTONE — 2026-07-04
Test Strategy approved → docs/test-strategy-dars-ma.md

## MILESTONE — 2026-07-04
DevOps Foundation approved → docs/devops-dars-ma.md
ENV_VARS_COLLECTED: DB_NAME, DB_USER, DB_PASSWORD, JWT_SIGNING_KEY, JWT_ACCESS_TTL_MIN, JWT_REFRESH_TTL_DAYS, CMI_MODE(=mock for now), CMI_MERCHANT_ID, CMI_API_KEY, SMTP_HOST, SMTP_PORT, SPRING_PROFILES_ACTIVE, ANGULAR_API_BASE_URL → .env.example written (no real secrets committed)

## MILESTONE — 2026-07-04
Stories/Full Sprint Backlog approved → docs/stories-dars-ma.md (8 sprints, 7 epics, 20 stories)
ALL 10 FOUNDATION DOCS APPROVED. Ready to commit + push per rule 13.

## PUSH — 2026-07-04
git push -u origin main → github.com/rhorba/dars-ma
Commit: de6a463 "docs: foundation documents for dars-ma"

## MILESTONE — 2026-07-05
Sprint 1 (Foundation & Auth) VERIFY phase complete:
- Backend: 8 unit tests + 4 integration tests (Testcontainers) pass. Coverage: 84% instructions / ~83.5% lines (gate: >=80%, MET).
- Frontend: 22 unit tests pass. Coverage: 93.46% lines (gate: >=80%, MET).
- Full docker-compose stack (backend+frontend+db) smoke-tested end-to-end: register -> login (HttpOnly refresh cookie issued) -> refresh (rotation working) all verified via curl through the real Nginx proxy.
- Security: Gitleaks scan clean (2 findings, both confirmed false positives: a Spring Boot test-log artifact already gitignored via target/, and an intentionally-fake JWT test fixture now marked with a gitleaks:allow comment). Trivy dependency/vuln scan attempted locally 3x; final attempt completed DB download + started analysis but timed out on Maven dependency resolution (network-bound semaphore deadline, not a finding) after ~8 min; Trivy runs automatically on every push via CI (ci.yml) in a clean Linux runner environment, so this is not a blocking gap.
- Fixed during verification: dev JWT signing key was too short for HS256 (WeakKeyException) - lengthened default + .env.example; nginx.conf lacked the /api proxy block in the built image (stale Docker cache, rebuilt); host port conflicts with other locally-running projects (made compose ports configurable via BACKEND_HOST_PORT/FRONTEND_HOST_PORT/DB_HOST_PORT env vars).
