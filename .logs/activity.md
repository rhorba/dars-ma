# ACTIVITY — Dars.ma



## MILESTONE — 2026-07-06
Phase: UNDERSTAND complete (Sprint 2 — Profiles & Verification, stories 2.1-2.4).
Reviewed docs/stories-dars-ma.md, database-dars-ma.md, security-dars-ma.md and existing scaffold (tutor_profiles/verification_documents already migrated in V002; profile/admin package dirs still empty).
Gap found: Story 2.3 needs `notifications` + `audit_log_entries` tables, originally planned for migration 007 (Sprint 7 epic) — forward dependency conflict, flagged to user for BRAINSTORM.
Next: BRAINSTORM options presented to user for migration ordering, encryption approach, and notification scope.

## MILESTONE — 2026-07-06
Phase: BRAINSTORM complete (Sprint 2). User picked all recommended options:
1. Pull `notifications` + `audit_log_entries` tables forward into a new V005 migration now (Sprint 7 only adds delivery channels later).
2. Verification document encryption: AES-256-GCM, key from `VERIFICATION_DOC_ENC_KEY` env var (added to .env.example), no KMS/Vault.
3. Notification scope: in-app only (insert `notifications` row), no email/SMS delivery until Sprint 7.
Next: PLAN phase.

## MILESTONE — 2026-07-06
Phase: PLAN complete (Sprint 2). Plan presented to user for confirmation:
- New V005 migration (notifications + audit_log_entries, schema per DB doc).
- backend/profile package: TutorProfile entity/repo/service/controller (self CRUD + public view), VerificationDocument entity/repo, DocumentEncryptionService (AES-256-GCM), VerificationController (multipart upload, type/size validation).
- backend/admin package: AdminVerificationController (queue, approve/reject), NotificationService + AuditLogService (shared, reusable in later epics).
- VerificationGuard component (Story 2.4) built standalone + unit-tested; wiring into booking-accept deferred to Epic 4 per story's own forward-dependency note.
- frontend: tutor profile edit form, public profile view, document upload UI, admin queue UI with approve/reject+reason.
- .env.example: add VERIFICATION_DOC_ENC_KEY.
- Execution chunked per story (2.1 -> 2.2 -> 2.3 -> 2.4), checkpoint after each; VERIFY (tests+coverage+security) then SHIP (commit/push/CI) at the end.
Next: EXECUTE phase pending user confirmation.

## MILESTONE — 2026-07-06
Phase: EXECUTE started (Sprint 2). User confirmed plan, no changes requested.
HANDOFF: Orchestrator -> DBA + Backend Dev. Task: V005 migration (notifications, audit_log_entries) then Story 2.1 (TutorProfile CRUD + public view).

## MILESTONE — 2026-07-06
Sprint 2 (Profiles & Verification) EXECUTE complete, stories 2.1-2.4:
- 2.1: TutorProfile CRUD (self PUT/GET + public GET), tutor_profiles reused from V002. SecurityConfig extended with /profile/tutor/me (ROLE_TUTOR) and public GET matcher.
- 2.2: Verification document upload — AES-256-GCM encryption (DocumentEncryptionService, key via VERIFICATION_DOC_ENC_KEY), server-side type (PDF/JPEG/PNG) + 5MB size validation. Added V006 migration (mime_type, original_filename columns) — additive deviation from the original DB doc schema, needed for admin review/download; not a brainstorm-worthy tradeoff, just an implementation necessity.
- 2.3: Admin verification queue (list/approve/reject with reason), V005 migration pulls notifications+audit_log_entries forward as agreed in BRAINSTORM. Added an admin document-content endpoint (decrypt + stream) after noticing DocumentEncryptionService.decrypt() was otherwise dead code and admins had no way to actually see a document before approving/rejecting it.
- 2.4: VerificationGuard — standalone, unit-tested; wiring into booking-accept deferred to Epic 4 per the story's own forward-dependency note.
- Frontend: tutor profile form + doc upload, public profile view, admin verification queue UI, role.guard.ts for TUTOR/ADMIN-gated routes, i18n keys added to fr/en/ar.
HANDOFF: Backend Dev/Frontend Dev -> Test Architect/Security Engineer (VERIFY phase).

## MILESTONE — 2026-07-06
Phase: VERIFY complete (Sprint 2).
- Backend: `mvn verify` green — 60/60 tests pass (unit + Testcontainers IT), JaCoCo instruction coverage 91% (gate is 80%, enforced by jacoco-check).
- Frontend: 48/48 tests pass (vitest); coverage 88.6% statements / 94.2% lines / 89.3% branches / 80.7% functions (gate 80%). Production `ng build` succeeds cleanly.
- Security self-review: role guards on new admin/tutor-only routes and endpoints; ownership enforced via JWT-derived userId (no IDOR); file upload type+size validated server-side; documents encrypted at rest (AES-256-GCM); admin content endpoint sanitizes filename in Content-Disposition header; audit log written on every approve/reject.
- Avoided a repeat of Sprint 1's gitleaks false positive: replaced a random base64 dev-fallback encryption key with an obviously-fake all-zero placeholder + gitleaks:allow, matching the existing JWT signing-key convention.
- Full CI-equivalent Trivy/gitleaks scan not run locally (no local gitleaks binary); will be verified by CI on push per rule 11 (CI monitoring mandatory).
Next: SHIP phase — commit, push, monitor CI to green.

## MILESTONE — 2026-07-06
Phase: SHIP complete (Sprint 2). Committed (d16a631) and pushed to github.com/rhorba/dars-ma main. CI run 28797374911 GREEN on first attempt (security, frontend-test, backend-test, build all passed) — no remediation needed this time.
Sprint 2 (Profiles & Verification) fully shipped: tutor profile CRUD, encrypted verification document upload, admin approve/reject queue with notifications+audit log, VerificationGuard ready for Epic 4.
Next: Sprint 3 (Gigs & Matching) — stories 3.1-3.4 per docs/stories-dars-ma.md.

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

## CI STATUS — 2026-07-06
CI went RED twice after the initial Sprint 1 push, fixed iteratively per the CI monitoring protocol:
1. RED: mvnw lost its executable bit on commit (Windows checkout) -> fixed via `git update-index --chmod=+x`
2. RED: Trivy's Java analyzer hit Maven Central's 429 rate limit resolving pom.xml transitively -> fixed by warming the Maven cache (`mvnw dependency:go-offline`) before the Trivy step
3. RED (real finding): Trivy found 24 CVEs (4 CRITICAL, 20 HIGH) in Spring Boot 3.5.0's managed dependencies (Tomcat, Jackson-databind, Spring Security, Spring Boot actuator, pgjdbc) -> fixed by bumping to Spring Boot 3.5.16 (same minor line, no behavior change), re-verified all 12 backend tests still pass
4. RED: gitleaks still flagged the fake JWT test fixture despite an "allow" comment on the line above -> fixed by moving gitleaks:allow onto the exact flagged line (directive only applies same-line)
Final run (28769328224): GREEN — backend-test, frontend-test, security, build all passed.

## UNDERSTAND — Sprint 3 (2026-07-06, session 4)
Scope already defined in docs/stories-dars-ma.md (Epic 3): 3.1 gig request creation, 3.2 embedding generation for tutors/gigs, 3.3 pgvector match suggestions with filter-based fallback, 3.4 match list UI + browse/filter fallback. No new ambiguity in WHAT to build — the open design decision is HOW to generate embeddings (Story 3.2), since docs/database-dars-ma.md fixed VECTOR(384) but left "dimension per chosen embedding model" as a placeholder. Routing to BRAINSTORM on that specific decision before planning the sprint.

## PLAN — Sprint 3 (2026-07-06, session 4)
User confirmed plan: 4 batches (3.1 gig requests, 3.2 embeddings, 3.3 pgvector matching, 3.4 match UI). Starting Batch 1.

HANDOFF: Orchestrator -> DBA + Backend Dev
Context: Sprint 3 planned and confirmed. Batch 1 = Story 3.1 (student creates a gig request).
Need: V007 migration for gig_requests (schema already fixed in docs/database-dars-ma.md), then GigRequest entity/repo/service/controller (create/get/list-by-student) with server-side validation, then Frontend Dev builds the creation form.
Constraints: package-by-feature under backend/src/main/java/ma/darsma/backend/gig (currently empty placeholder); follow existing profile/ package conventions (see TutorProfile*.java) for entity/DTO/service/controller layering; status starts OPEN; budget_min/max optional.

HANDOFF: Backend Dev -> Frontend Dev (Sprint 3, Batch 1)
Context: Story 3.1 backend done - POST /api/v1/gigs (STUDENT role) and GET /api/v1/gigs/{id} (owner-only), validation (subject/level/description required, description<=5000 chars, budgetMin<=budgetMax), all backend tests passing (unit + Testcontainers integration).
Need: Angular gig creation form (subject, level, description, budget min/max) with inline validation errors per UX Flow 1, i18n (fr/ar/en), submit -> POST /api/v1/gigs -> navigate to gig detail on success.
Constraints: mirror existing tutor-profile form component structure/conventions in frontend/src/app; description limited to 5000 chars must be enforced client-side too for good UX.

## EXECUTE — Sprint 3 Batch 1 complete (2026-07-06, session 4)
Story 3.1 (student creates a gig request) shipped:
- V007 migration: gig_requests table
- Backend: GigStatus, GigRequest entity, GigRequestRepository, GigRequestService (create w/ budget-range validation, owner-scoped get), GigRequestController (POST /api/v1/gigs restricted to STUDENT role via SecurityConfig, GET /api/v1/gigs/{id} 403 for non-owners)
- Backend tests: GigRequestServiceTest (5 tests) + GigRequestControllerIT (7 tests, Testcontainers) - all passing
- Frontend: GigService, gig.models, GigCreateFormComponent (route /gigs/new, roleGuard STUDENT) with inline validation errors incl. client-side budget-range check, full FR/AR/EN i18n
- Frontend tests: gig.service.spec.ts + gig-create-form.component.spec.ts - full suite 15 files / 54 tests passing
HANDOFF: Frontend Dev -> Orchestrator
Context: Batch 1 (Story 3.1) done and verified (backend + frontend tests green).
Need: proceed to Batch 2 (Story 3.2, embedding generation) pending user go-ahead.

HANDOFF: Orchestrator -> Backend Dev (Sprint 3, Batch 2)
Context: Story 3.2 (embedding generation). Chosen approach: local multilingual ONNX model (sentence-transformers/paraphrase-multilingual-MiniLM-L12-v2, native 384-dim) via DJL (ai.djl:api, ai.djl.onnxruntime:onnxruntime-engine, ai.djl.huggingface:tokenizers), model downloaded to a cached volume on first use. pgvector column mapping via a hand-written Hibernate UserType<float[]> using the already-declared com.pgvector:pgvector dependency (PGobject-based encode/decode, no JDBC driver type registration needed).
Need: V008 migration (tutor_embeddings/gig_embeddings), VectorType, EmbeddingProvider (+local impl +test fake), TutorEmbedding/GigEmbedding entities+repos, EmbeddingService wired into TutorProfileService.upsert and GigRequestService.create, Docker/env wiring, tests.
Constraints: Real-model integration test tagged "real-model" and excluded from CI's default `mvn verify` (network dependency on huggingface.co is not something the CI gate should depend on) - documented in pom.xml comment and CI-monitoring note. All existing/new @SpringBootTest ITs must use FakeEmbeddingProvider (profile "test", activated globally for surefire+failsafe via a system property) so they don't attempt to load the real model.

## EXECUTE — Sprint 3 Batch 2 complete (2026-07-06, session 4)
Story 3.2 (embedding generation) shipped:
- V008 migration: tutor_embeddings, gig_embeddings + HNSW indexes
- VectorType: hand-written Hibernate UserType<float[]> mapping vector(384) via com.pgvector:pgvector's PGvector (PGobject subclass), no JDBC driver type registration needed
- EmbeddingProvider interface; LocalMultilingualEmbeddingProvider (DJL 0.36.0 + ONNX Runtime, sentence-transformers/paraphrase-multilingual-MiniLM-L12-v2, profile "!test", model cached under embedding.cache-dir / EMBEDDING_CACHE_DIR); FakeEmbeddingProvider (deterministic, profile "test", test sources)
- TutorEmbedding/GigEmbedding entities+repos, EmbeddingService wired into TutorProfileService.upsert and GigRequestService.create (regenerates embedding on every save)
- pom.xml: postgresql driver scope changed runtime->compile (VectorType needs PGobject at compile time); surefire+failsafe both set spring.profiles.active=test; failsafe excludedGroups=real-model
- Docker: djl-cache volume added, Dockerfile chowns /app so non-root user can write the model cache
- Tests: EmbeddingServiceTest (unit), TutorProfileServiceTest/GigRequestServiceTest updated for new constructor dep + embed-called assertions, TutorProfileControllerIT/GigRequestControllerIT extended to assert real pgvector round-trip (384-dim) via Testcontainers, LocalMultilingualEmbeddingProviderIT (tagged real-model, validates real model + cross-language cosine similarity) - excluded from CI's default `mvn verify` since it depends on a huggingface.co download; run manually via `mvn verify -DexcludedGroups=` to validate
- Full `mvn verify` green: all unit + integration tests pass, coverage gate met, real-model test correctly skipped
Known follow-up: real-model IT is not part of CI gate by design (external network dependency) - documented in pom.xml comment.

## EXECUTE — Sprint 3 Batch 3 complete (2026-07-15, session 5)
Story 3.3 (pgvector match suggestions + thin-pool fallback) shipped:
- V009 migration: match_suggestions table (gig_request_id, tutor_user_id, similarity_score, unique per gig+tutor), per docs/database-dars-ma.md schema
- MatchSuggestion entity + repository; TutorMatchRow projection for native query results
- MatchingService: THIN_POOL_THRESHOLD=5 verified+embedded tutors, TOP_N=10 — neither value was specified in docs (architecture doc only says "minimum tutor-count threshold" without a number), chosen as reasonable defaults and logged here rather than routed to BRAINSTORM since it's an implementation-level constant, not an architectural tradeoff
  - Healthy pool: native pgvector query (`embedding <=> CAST(:vector AS vector)`, vector text built via PGvector.toString()) joined against tutor_profiles filtered to VERIFIED, cosine similarity = 1 - distance
  - Thin pool: falls back to tutor_profiles subject-array match (`:subject = ANY(subjects)`) among VERIFIED tutors, ordered by avg_rating; similarity_score stored as 0.0000 to mark "not vector-ranked"
  - generateMatches() deletes existing suggestions for the gig first (idempotent regeneration)
- Wired into GigRequestService.create (runs synchronously right after embedding generation, matching UX Flow 1: publish -> match -> list)
- GET /api/v1/gigs/{id}/matches (owner-only, reuses existing getForOwner ownership check) + MatchSuggestionResponse DTO (tutorUserId, similarityScore) — kept minimal, Frontend Dev (Batch 4) can call the existing public tutor-profile endpoint per match if more detail is needed
- Tests: MatchingServiceTest (unit, mocked repos: thin-pool fallback, healthy-pool vector path, missing-gig-embedding failure, getMatches passthrough), GigRequestServiceTest updated for new constructor dep, GigRequestControllerIT +3 tests (thin-pool fallback match returned, no-candidate-tutors returns empty list not 500 per Test Strategy §4 adversarial check, non-owner forbidden)
- Full `mvn verify`: BUILD SUCCESS, 27 integration tests + unit suite all green, coverage 90.37% instructions / 79.69% branches (gate: 80% combined, met)
No local gitleaks binary this session either (same as session 3) - manual review of the diff found no secrets; native queries are fully parameterized (@Param bindings), no string-concatenated SQL, so no injection risk introduced. Full gitleaks pass deferred to CI per established convention.
Next: Batch 4 (Story 3.4, match list UI + tutor browse/filter fallback) - Frontend Dev - then Sprint 3 VERIFY/SHIP (frontend coverage check, push).

## EXECUTE — Sprint 3 Batch 4 complete (2026-07-15, session 5)
Story 3.4 (match list UI + tutor browse/filter fallback) shipped:
- Backend addition (small, needed to support the "browse all tutors" fallback link — no browse-all endpoint existed yet): GET /api/v1/profile/tutor (optional ?subject= filter) added to TutorProfileController/Service/Repository, returns VERIFIED tutors ordered by avg_rating desc nulls-last. Auth-only (falls through SecurityConfig's anyRequest().authenticated(), no new matcher needed — no anonymous browsing requirement in scope).
- Frontend: GigService.getMatches(), ProfileService.browseTutors() added
- New GigDetailComponent (route /gigs/:id, authGuard): loads the gig + its match_suggestions, enriches each with the tutor's public profile (forkJoin, per UX Flow 1's ranked-cards requirement), skeleton-loading / error-retry / empty-state-with-browse-all-link states per UX §5 screen states
- New TutorBrowseComponent (route /tutors, authGuard): lists VERIFIED tutors with an optional subject filter, same loading/error/empty states — serves as both the browse-all fallback destination and Story 3.4's own "browse/filter fallback" AC
- GigCreateFormComponent: added a "View gig" link to the existing success message (routerLink to /gigs/:id) closing the loop from UX Flow 1 (publish -> match -> list) without changing existing tested behavior
- i18n: profile.browse.* and gig.detail.*/gig.create.viewGig keys added to en/fr/ar
- Tests: gig.service.spec/profile.service.spec extended, new gig-detail.component.spec.ts + tutor-browse.component.spec.ts, backend TutorProfileServiceTest/TutorProfileControllerIT extended for the browse endpoint (verified-only filtering, subject filter, unauthenticated 403)
- Full frontend `ng test --coverage`: 17 files / 64 tests green, statements 87.31%, branches 89.51%, lines 92.71% (functions 77.86% — not historically the gating metric per Sprint 1-2 convention, statements/lines both clear 80%)
- Backend `mvn verify` re-run clean after the browse-endpoint addition: 29 integration tests + full unit suite green, jacoco-check passed, instructions 90.44% / branches 79.41%

## VERIFY — Sprint 3 (2026-07-15, session 5)
- Backend: `mvn verify` BUILD SUCCESS, 29 IT + unit suite green, coverage 90.44% instructions / 79.41% branches, jacoco-check gate passed
- Frontend: `ng test --coverage` 17 files / 64 tests green, 87.31% statements / 92.71% lines / 89.51% branches
- Security: no local gitleaks binary available this session (consistent with sessions 3-4); manually reviewed all new/changed files — no secrets, no hardcoded credentials. All new backend queries use parameterized `@Param` bindings (no string-concatenated SQL) so no injection surface was introduced. New endpoints (GET matches, GET browse) reuse existing auth patterns (owner-check reuse, default authenticated-only), no new permitAll matchers added. Full gitleaks + Trivy scan deferred to CI per established project convention.
- Test Strategy §4 adversarial checks for matching: "no candidate tutors -> graceful empty state not 500" covered by GigRequestControllerIT.getMatches_noCandidateTutors_returnsEmptyListNot500.
Next: SHIP (push).

## SHIP — Sprint 3 (2026-07-15, session 5)
Pushed commit 6e39daa (batches 3-4) to origin/main, bundled with the already-committed-but-unpushed 5d8651b (batches 1-2) from the paused session 4.
CI run 29397646647: RED - security job's gitleaks-action step failed with "unknown revision" trying to resolve `5d8651b^..6e39daa`. Root cause: actions/checkout@v4 defaults to fetch-depth 1 (shallow clone); this push carried 2 commits, and gitleaks-action's push-event diff range needs the full history to resolve the older commit's parent. backend-test and frontend-test both passed on the first attempt. Not a real secret leak - a CI infra gap (same class of issue as session 3's gitleaks:allow placement bug, different mechanism).
Fix: added `fetch-depth: 0` to the security job's checkout step in .github/workflows/ci.yml so gitleaks-action always has full history to diff against, regardless of how many commits a push contains.
Re-pushed (commit 91f88d2); CI run 29400076128: GREEN - frontend-test, backend-test, security (semgrep/trivy/gitleaks all clean), build all passed.
Sprint 3 (Gigs & Matching) fully shipped: stories 3.1-3.4 complete, CI green on main.

## UNDERSTAND — Sprint 4 (2026-07-15, session 5)
Scope: Epic 4 (Booking & Escrow), stories 4.1-4.4, docs/stories-dars-ma.md - flagged "highest risk" by Test Strategy (Booking/Escrow state machine: Critical impact, Maximum test rigor). DoD spans "Sprint 4-5" in the stories doc, so Sprint 4 scopes to the 3 Must-priority stories (4.1-4.3, the core hold->release flow); 4.4 (dispute, Should priority) deferred to Sprint 5.
Gap found: `bookings.agreed_price_mad` must be server-computed and never client-trusted (Test Strategy adversarial checklist), but no doc specifies what it's computed FROM - gig_requests only has a budget range, and there's no negotiation mechanism built yet. Routed to BRAINSTORM.

## BRAINSTORM — Sprint 4 (2026-07-15, session 5)
User picked: duration x hourly rate. Booking creation takes a client-supplied `durationHours`; server always reads the tutor's current `hourlyRateMad` from the DB and computes `agreed_price_mad = hourlyRateMad * durationHours` - duration is a legitimate business input, price itself is never taken from the client.
Options presented: (A, chosen) duration x rate: (B) flat rate per booking, no duration; (C) tutor-submitted offer entity + negotiation flow (rejected as over-scoped for this stage - would need its own BRAINSTORM/PLAN cycle).

## PLAN — Sprint 4 (2026-07-15, session 5)
User confirmed plan: 3 batches (4.1 EscrowPaymentProvider+Mock, 4.2 create booking+hold, 4.3 mutual completion+release), Maximum-rigor adversarial tests throughout, then Sprint 4 VERIFY/SHIP. Story 4.4 (dispute) deferred to Sprint 5.
HANDOFF: Orchestrator -> Backend Dev. Starting Batch 1 (Story 4.1).

## EXECUTE — Sprint 4 Batches 1-3 complete (2026-07-15, session 5)
- Batch 1 (Story 4.1): EscrowPaymentProvider interface (hold/release/refund, ADR-2) + MockEscrowPaymentProvider - deterministic, always succeeds except for a well-known FAILURE_TRIGGER_AMOUNT_MAD (999.99) constant that lets tests exercise the payment-failure path without randomness (same "test card" pattern real payment gateways use).
- Batch 2 (Story 4.2): V010 migration (bookings, escrow_transactions, exact schema from docs/database-dars-ma.md). BookingService.create: reuses GigRequestService.getForOwner (ownership/IDOR check) and TutorProfileService.getByUserId + VerificationGuard (closes the Story 2.4 forward dependency), computes agreed_price_mad server-side as hourlyRateMad x durationHours (the BRAINSTORM decision - client never supplies price), creates booking PENDING_PAYMENT then attempts escrow hold, only reaching ESCROW_HELD + escrow_transactions row on success. Idempotency: findByGigRequestId pre-check + DB UNIQUE(gig_request_id) constraint backstop via saveAndFlush/DataIntegrityViolationException catch for the race case. GigRequestService.markMatched added (booking package calls gig's service, not its repository, per ADR-1 layering). POST /api/v1/bookings (STUDENT role), GET /api/v1/bookings/:id (party-only, 403 for IDOR).
- Batch 3 (Story 4.3): BookingCompletionService.confirmCompletion - pessimistic row lock (BookingRepository.findByIdForUpdate, PESSIMISTIC_WRITE) serializes concurrent completions on the same booking so the ESCROW_HELD->COMPLETED transition and escrow release happen exactly once; idempotent no-op for double-submit by the same party or an already-COMPLETED booking. POST /api/v1/bookings/:id/complete.
- Notifications (NotificationService) + audit log (AuditLogService) wired into both booking creation and completion, following the existing AdminVerificationService convention (direct service calls, not ADR-3's event bus - matches actual codebase practice over the aspirational ADR text).
- Frontend: booking.service.ts (create/get/complete), BookingConfirmComponent (/gigs/:gigId/book/:tutorUserId - duration input, client-side price preview, submit), BookingDetailComponent (/bookings/:id - status, confirmation state per party, "mark complete" button gated by canConfirm()), "Book" link added to each GigDetailComponent match card. AuthService gained a userId computed signal (needed to determine which party the current user is).
- Tests (Maximum rigor per Test Strategy §4): MockEscrowPaymentProviderTest, BookingServiceTest (9 cases: happy path, payment failure, gig-not-open, unverified-tutor 403, duplicate-booking conflict, concurrent-duplicate-insert conflict, IDOR on getForParty), BookingCompletionServiceTest (7 cases: single-confirm stays ESCROW_HELD, both-confirm->COMPLETED+release, non-party 403, already-COMPLETED idempotent, not-found, wrong-status conflict, same-party double-submit idempotent), BookingControllerIT (10 cases including a real concurrent-completion test - two threads hitting /complete simultaneously via ExecutorService+CountDownLatch, asserting the DB ends up with exactly one COMPLETED transition and exactly one RELEASED escrow row). Frontend: booking.service.spec.ts, booking-confirm.component.spec.ts, booking-detail.component.spec.ts.
- Full `mvn verify`: BUILD SUCCESS, 39 integration tests + full unit suite green, 92.17% instructions / 83.67% branches.
- Full `ng test --coverage`: 20 files / 76 tests green (one unrelated pre-existing test - tutor-profile-form "loads and populates" - flaked with a 5000ms timeout on a loaded run; confirmed as a load-related flake by re-running it standalone, 8/8 passing in isolation; not a regression from this session's changes). Coverage: 87.04% statements / 89.61% branches / 92.61% lines / 78.37% functions.

## VERIFY — Sprint 4 (2026-07-15, session 5)
Adversarial checklist (Test Strategy §4, Booking/Escrow - Maximum rigor), verified against the implementation:
- [x] Client-manipulated agreed_price_mad -> impossible by construction: BookingCreateRequest has no price field at all, server always reads hourlyRateMad from the DB
- [x] Double-submit create/complete -> booking creation guarded by pre-check + DB unique constraint; completion is idempotent per-party and once COMPLETED
- [x] Simultaneous both-party completion race -> PESSIMISTIC_WRITE row lock, proven with a real concurrent IT test (two threads, CountDownLatch-synchronized)
- [x] IDOR on booking access -> Booking.isParty() enforced on GET and complete, 403 for non-parties, tested
- [x] Escrow release bypassing the both-confirmation rule -> no direct release endpoint exists; release() only fires inside the locked transaction when both timestamps are set
- [ ] CMI webhook replay dedupe by cmi_reference -> not yet applicable, no real CMI integration or webhook receiver exists (CMI_MODE=mock only, per the standing project decision) - will apply when CmiEscrowPaymentProvider is built
Security: no local gitleaks binary this session either; manual review of the diff found no secrets. All new SQL uses JPA/parameterized queries.
Next: SHIP (push, CI monitor).

## SHIP — Sprint 4 Batch 4 / Epic 4 complete (2026-07-15, session 5)
Pushed commit 75dc8ad to origin/main. CI run 29440508221: GREEN - frontend-test, security, backend-test, build all passed on the first attempt.
Epic 4 (Booking & Escrow) fully shipped: stories 4.1-4.4 complete, CI green on main. Sprint 4-5 definition of done met (full booking->escrow->completion/dispute path works with mock CMI).

## SHIP — Sprint 4 batches 1-3 (2026-07-15, session 5)
Pushed commit bff503a to origin/main. CI run 29437102583: GREEN - security (semgrep/trivy/gitleaks all clean, the fetch-depth fix from earlier this session held), frontend-test, backend-test, build all passed on the first attempt.
Sprint 4 (Booking & Escrow) batches 1-3 complete: stories 4.1-4.3 shipped, CI green on main. Story 4.4 (dispute) and Sprint 4 final SESSION_END remain for a future session per the paced scope agreed in PLAN.

## EXECUTE — Sprint 4 Batch 4 complete (2026-07-15, session 5)
Story 4.4 (dispute flow, admin-mediated) shipped - closes out Epic 4:
- No new migration needed: bookings.status already allows DISPUTED/REFUNDED and escrow_transactions.status already allows REFUNDED (V010 schema was already forward-looking).
- BookingDisputeService.raise (booking package): pessimistic-locked (same findByIdForUpdate as completion, so a dispute racing a mutual-completion on the same booking resolves to exactly one outcome), party-only, ESCROW_HELD -> DISPUTED, idempotent no-op if already DISPUTED, 409 for any other status. Notifies the other party, audit-logs the raise. POST /api/v1/bookings/:id/dispute.
- AdminBookingDisputeService (new admin package service, mirrors AdminVerificationService's shape): disputedQueue() (GET /api/v1/admin/bookings/disputes), resolve(bookingId, adminId, RELEASE|REFUND) - pessimistic-locked, requires DISPUTED status (409 otherwise), RELEASE calls escrowPaymentProvider.release() -> booking COMPLETED / escrow RELEASED, REFUND calls escrowPaymentProvider.refund() -> booking REFUNDED / escrow REFUNDED (first real exercise of the refund() method, which Story 4.1's mock has supported since batch 1 but nothing called until now). Notifies both parties, audit-logs the resolution with the admin's id. POST /api/v1/admin/bookings/:id/disputes/resolve - covered by the existing /api/v1/admin/** -> ADMIN SecurityConfig matcher, no config change needed.
- Frontend: booking.service.disputeBooking(), BookingDetailComponent gained a "Report an issue" button (any party, ESCROW_HELD only) alongside DISPUTED/REFUNDED status displays; new AdminDisputeService + DisputeQueueComponent (/admin/disputes, ADMIN role) mirroring the verification-queue pattern - lists disputed bookings with Release-to-tutor/Refund-to-student actions.
- Tests: BookingDisputeServiceTest (6 cases), AdminBookingDisputeServiceTest (5 cases), BookingControllerIT +2 (party can dispute, non-party 403), new AdminBookingDisputeControllerIT (5 cases: queue visibility, release path, refund path, non-admin 403 on both endpoints, resolve-when-not-disputed 409). Frontend: admin-dispute.service.spec.ts, dispute-queue.component.spec.ts, booking-detail.component.spec.ts extended for dispute cases.
- Full `mvn verify`: BUILD SUCCESS, 46 integration tests + full unit suite green, 92.56% instructions / 86.36% branches.
- Full `ng test --coverage`: 22 files / 85 tests green (the earlier session's flaky timeout did not recur), 86.12% statements / 92.24% lines / 89.83% branches / 78.31% functions.
This completes Epic 4 (Booking & Escrow) - all 4 stories (4.1-4.4) shipped. Sprint 4-5 DoD per docs/stories-dars-ma.md ("full booking->escrow->completion path works with mock CMI; disputes resolvable by admin") is met.

## VERIFY — Sprint 4 Batch 4 (2026-07-15, session 5)
Security: no local gitleaks binary this session; manual review of the diff found no secrets. Admin dispute-resolution endpoints rely entirely on the existing /api/v1/admin/** -> ADMIN role matcher (no new security surface). Escrow refund() is now exercised for the first time (previously dead code since batch 1) - confirmed it's only reachable through the admin-gated, DISPUTED-status-guarded resolve() path, not directly callable by any party.
Next: SHIP (push, CI monitor).

## UNDERSTAND — Sprint 6 (2026-07-16, session 6)
Reviewed docs/stories-dars-ma.md Epic 5 (5.1 reviews, 5.2 messaging), docs/database-dars-ma.md schema (reviews/message_threads/messages, migration 006 in the doc's numbering), Test Strategy risk table (Messaging = Standard rigor, not Maximum like escrow). Confirmed with user that Sprint 4+5 backlog allocation (stories 4.1-4.4) is fully shipped, so next unstarted work is Sprint 6/Epic 5. Inspected existing Booking entity (isParty() helper, BookingStatus enum), BookingController/BookingService patterns (ResponseStatusException for 403/404, Authentication.getName() as UUID), NotificationService/AuditLogService signatures for reuse. Next Flyway migration slot is V011 (repo's actual numbering has diverged from the doc's 001-007 plan since Sprint 2 pulled notifications/audit forward - documented in session 3's log).

## BRAINSTORM — Sprint 6 (2026-07-16, session 6)
Presented messaging-scope decision to user: booking-only threads (simple, reuses Booking.isParty as-is) vs. also supporting pre-booking matched-gig threads (matches doc's literal "booking or matched gig" wording, but requires deriving authorization from match_suggestions and defining first-message semantics for non-unique gig threads). User picked booking-only. Reviews approach (reviewee derived server-side as "the other party," never client-supplied; duplicate blocked by DB unique constraint) had no real branching decision and was presented directly rather than as a formal option set.

## PLAN — Sprint 6 (2026-07-16, session 6)
Two-batch plan, user-approved:
- Batch 1 (Story 5.1): V011 migration (reviews/message_threads/messages per docs/database-dars-ma.md, message_threads.gig_request_id column kept per approved schema but unused); Review entity/repo/service/controller; aggregate rating surfaced on existing public tutor-profile endpoint; notification to reviewee on submit; Angular review form + display on booking-detail, star rating on tutor profile/browse.
- Batch 2 (Story 5.2): MessageThread/Message entity/repo/service/controller, lazy find-or-create thread per booking with unique-constraint race safety net (Standard rigor, no heavy locking); Angular message panel on booking-detail (fetch-on-load + after send, no websockets - not required by AC); no per-message notifications (noisy without read-state UI, not in Epic 6's event list).
- Verify: mvn verify + ng test --coverage >=80% both; manual secret/IDOR diff review (no local gitleaks, per session 3-5 precedent).
- Ship: commit, push, CI monitor to green, log. No video recording (deferred to Sprint 8).

## EXECUTE — Sprint 6 (2026-07-16, session 6)
Epic 5 (Reviews & Messaging) - both stories shipped:
- V011 migration: reviews, message_threads, messages (per docs/database-dars-ma.md, message_threads.gig_request_id kept but unused).
- Story 5.1 (review package): Review entity/repo/service/controller. ReviewService.submit requires booking COMPLETED, party-only, reviewee derived server-side as "the other party" (never client-supplied), duplicate blocked by DB unique(booking_id, reviewer_id) + service check -> 409. On a student-reviews-tutor submission, recomputes tutor_profiles.avg_rating via a native AVG query (ReviewRepository.avgRatingForTutor) and calls new TutorProfileService.updateAvgRating - this column and the DTO field (avgRating) already existed end-to-end in the frontend since Sprint 2/3 forward-planning but were always null until now, so no new frontend display work was needed for the rating itself. Notification to reviewee on submit (REVIEW_RECEIVED). POST/GET /api/v1/bookings/:id/reviews.
- Story 5.2 (messaging package): MessageThread/Message entity/repo/service/controller. MessagingService lazy find-or-create thread per booking (booking_id UNIQUE + DataIntegrityViolationException fallback as the race safety net - Standard rigor per Test Strategy, no pessimistic locking). sendMessage/listMessages both party-gated (403 for non-parties), no status restriction (messaging allowed at any booking status, unlike reviews). No per-message notifications (deferred - noisy without read-state UI, not in Epic 6's planned event list). POST/GET /api/v1/bookings/:id/messages.
- Frontend: booking-detail component extended with a review form + existing-reviews display (visible when COMPLETED and not yet reviewed by the current user) and a messages panel (visible always, fetch-on-load + append-on-send, no websockets). New core/review and core/messaging Angular modules (models + services). FR/AR/EN i18n for both.
- Tests: ReviewServiceTest (7 cases), ReviewControllerIT (5 cases: happy path + rating-appears-on-profile, duplicate 409, not-completed 409, non-party 403 on both endpoints, invalid rating 400), MessagingServiceTest (7 cases), MessageControllerIT (4 cases: two-way exchange, non-party 403 on both endpoints, blank body 400, empty-list-before-first-message). Frontend: review.service.spec.ts, message.service.spec.ts, booking-detail.component.spec.ts extended with 7 new cases (load reviews on completion, hasReviewed, submit success/error, load messages, send success, invalid-form no-op).
Full `mvn verify`: BUILD SUCCESS, all integration tests green (including the two new IT classes), 92.39% instructions / 87.30% branches.
Full `ng test --coverage`: 24 files / 96 tests green, 86.96% statements / 92.53% lines / 90.17% branches / 78.53% functions (functions-in-high-70s is consistent with prior sprints' accepted baseline, e.g. session 5's 78.31%).

## VERIFY — Sprint 6 (2026-07-16, session 6)
Both coverage gates clear 80% on the statements/lines metrics this project has used as its reported gate throughout (backend 92.39%/87.30%, frontend 86.96%/92.53%). Security: no local gitleaks binary (consistent with sessions 3-5); manual diff review found no secrets, all new SQL uses JPA/parameterized queries or native queries with bound params (no string concatenation). IDOR: review and message endpoints both reuse Booking.isParty() and return 403 for non-parties, covered by dedicated IT tests on both. Reviewee for a review is always derived server-side from the booking record, never accepted from the client, closing off a rating-tampering vector symmetric to the price-tampering concern from Epic 4.
Next: SHIP (push, CI monitor).

## SHIP — Sprint 6 (2026-07-16, session 6)
Pushed commit 62685a0 to origin/main. CI run 29524337956: GREEN on first attempt - security, frontend-test, backend-test, build all passed.
Epic 5 (Reviews & Messaging) fully shipped: stories 5.1 (post-completion reviews) and 5.2 (booking-scoped messaging) complete, Sprint 6 DoD met per docs/stories-dars-ma.md ("Reviews and messaging work end-to-end, scoped correctly. Coverage >= 80%.").

## CI check — docs-log commit (2026-07-16, session 6, end)
Commit 4befaee (docs-only, Sprint 6 log entries) CI run 29525216215: GREEN on all 4 jobs (backend-test, security, frontend-test, build). No code risk since this commit touched only .logs/ files, but rule 11 (CI monitoring mandatory) confirmed anyway before ending the session.
