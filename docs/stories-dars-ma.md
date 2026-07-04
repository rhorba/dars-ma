# Stories: Dars.ma
**PRD**: docs/prd-dars-ma.md
**Architecture**: docs/architecture-dars-ma.md
**Database**: docs/database-dars-ma.md
**Test Strategy**: docs/test-strategy-dars-ma.md

Scope: Comprehensive (full README vision). Target: 8 sprints (~2 weeks each, within the 7-10 range).

---

## Epic 1: Foundation & Auth
Sets up the scaffold, CI/Docker, and authentication so every later feature has something to build on.

### Story 1.1: Project scaffold (backend + frontend + Docker)
**Priority**: Must | **Size**: L | **Specialist**: Backend Dev + Frontend Dev + DevOps
**Description**: As a developer, I want the Spring Boot + Angular + Docker Compose scaffold running, so that all future stories have a foundation.
**Acceptance Criteria**:
- [ ] Given a fresh clone, when `docker compose up` runs, then backend, frontend, and Postgres+pgvector containers start healthy
- [ ] Given the CI pipeline, when a PR is opened, then lint+test+security+build all run
**Technical Notes**: Architecture ADR-1 (package-by-feature), DevOps doc Docker Compose/Dockerfiles
**Dependencies**: None

### Story 1.2: Database migrations (Flyway) — all core tables
**Priority**: Must | **Size**: M | **Specialist**: Backend Dev / DBA
**Description**: As a developer, I want the schema from the Database Design doc applied via Flyway migrations, so that the app has a working data layer.
**Acceptance Criteria**:
- [ ] Given migrations 001-007 (per Database doc), when the app starts, then all tables/extensions/indexes exist
**Technical Notes**: docs/database-dars-ma.md migration plan
**Dependencies**: 1.1

### Story 1.3: User registration & login (JWT)
**Priority**: Must | **Size**: M | **Specialist**: Backend Dev
**Description**: As a user, I want to register as Student or Tutor and log in, so that I can access the platform.
**Acceptance Criteria**:
- [ ] Given valid registration data, when I submit, then a user is created with hashed password and correct role
- [ ] Given valid credentials, when I log in, then I receive an access + refresh JWT pair
- [ ] Given 5 failed login attempts, when I try again, then I get exponential backoff (Security Baseline)
**Technical Notes**: Security doc Auth Strategy; FR-1
**Dependencies**: 1.2

### Story 1.4: Angular auth shell + route guards + i18n skeleton
**Priority**: Must | **Size**: M | **Specialist**: Frontend Dev
**Description**: As a user, I want login/register screens and protected routing, so that role-based areas are accessible only when authenticated.
**Acceptance Criteria**:
- [ ] Given no token, when I visit a protected route, then I'm redirected to login
- [ ] Given `preferred_lang=ar`, when I load the app, then `dir="rtl"` is applied (UI doc)
**Technical Notes**: Architecture ADR-4 (standalone components); UI doc §5 RTL
**Dependencies**: 1.3

**Definition of Done (Sprint 1)**: Docker Compose runs all services; CI green; a user can register/login; RTL direction switches correctly. Coverage ≥ 80% on written code.

---

## Epic 2: Profiles & Verification
### Story 2.1: Student profile (minimal) & Tutor profile CRUD
**Priority**: Must | **Size**: M | **Specialist**: Backend Dev + Frontend Dev
**Acceptance Criteria**:
- [ ] Given a logged-in tutor, when they fill subjects/rate/bio, then `tutor_profiles` is created/updated
- [ ] Given a public visitor, when they view a tutor profile, then verification status and rating are visible
**Technical Notes**: FR-2; database `tutor_profiles`
**Dependencies**: 1.3, 1.4

### Story 2.2: Verification document upload
**Priority**: Must | **Size**: M | **Specialist**: Backend Dev + Frontend Dev
**Acceptance Criteria**:
- [ ] Given a tutor uploads a diploma (PDF/image, size-limited), when submitted, then a `verification_documents` row is created, encrypted at rest, status PENDING
- [ ] Given a wrong file type or oversized file, when uploaded, then it's rejected server-side (adversarial checklist)
**Technical Notes**: Security doc §6 file upload rules; `verification_documents.encrypted_blob`
**Dependencies**: 2.1

### Story 2.3: Admin verification queue (approve/reject)
**Priority**: Must | **Size**: M | **Specialist**: Backend Dev + Frontend Dev
**Acceptance Criteria**:
- [ ] Given a PENDING document, when admin approves, then `verification_status` becomes VERIFIED, notification sent, audit log entry written
- [ ] Given admin rejects, when submitted with a reason, then tutor sees REJECTED + reason and can resubmit
**Technical Notes**: UX Flow 3; `audit_log_entries`
**Dependencies**: 2.2

### Story 2.4: Unverified tutor cannot accept bookings (guard)
**Priority**: Must | **Size**: S | **Specialist**: Backend Dev
**Acceptance Criteria**:
- [ ] Given a PENDING or REJECTED tutor, when they attempt to accept a match, then request is rejected with a clear message
**Dependencies**: 2.3, (forward dependency on Epic 4 booking creation — implemented as a guard reused there)

**Definition of Done (Sprint 2)**: Tutors can build a profile and submit verification; admin can approve/reject; unverified tutors are blocked from bookings. Coverage ≥ 80%.

---

## Epic 3: Gigs & Matching
### Story 3.1: Student creates a gig request
**Priority**: Must | **Size**: M | **Specialist**: Backend Dev + Frontend Dev
**Acceptance Criteria**:
- [ ] Given valid subject/level/description/budget, when submitted, then `gig_requests` row created with status OPEN
- [ ] Given invalid input, when submitted, then inline validation errors shown (UX Flow 1)
**Dependencies**: 1.4

### Story 3.2: Generate embeddings for tutors and gigs
**Priority**: Must | **Size**: L | **Specialist**: Backend Dev
**Description**: As the system, I want to generate vector embeddings for tutor profiles and gig requests, so that matching can run.
**Acceptance Criteria**:
- [ ] Given a tutor profile is created/updated, when saved, then `tutor_embeddings` is (re)generated
- [ ] Given a gig request is created, when saved, then `gig_embeddings` is generated
**Technical Notes**: Architecture "matching" module; System Design SDR-3
**Dependencies**: 2.1, 3.1

### Story 3.3: pgvector match suggestions
**Priority**: Must | **Size**: L | **Specialist**: Backend Dev
**Acceptance Criteria**:
- [ ] Given a gig request, when matching runs, then top-N `match_suggestions` are created ranked by cosine similarity
- [ ] Given a thin tutor pool (below threshold), when matching runs, then it falls back to filter-based search (subject match) instead of pure vector search
**Technical Notes**: PRD risk mitigation; Test Strategy §4 matching adversarial checks
**Dependencies**: 3.2

### Story 3.4: Match list UI + tutor browse/filter (fallback)
**Priority**: Must | **Size**: M | **Specialist**: Frontend Dev
**Acceptance Criteria**:
- [ ] Given matches exist, when student views a gig, then ranked tutor cards are shown
- [ ] Given no matches, when student views a gig, then empty state + "browse all tutors" fallback shown (UX §5 screen states)
**Dependencies**: 3.3

**Definition of Done (Sprint 3)**: Students can post gigs and see matched or browsed tutors end-to-end. Coverage ≥ 80%.

---

## Epic 4: Booking & Escrow (highest risk — Test Strategy: Maximum rigor)
### Story 4.1: EscrowPaymentProvider interface + Mock implementation
**Priority**: Must | **Size**: M | **Specialist**: Backend Dev
**Acceptance Criteria**:
- [ ] Given `CMI_MODE=mock`, when a booking is created, then `MockEscrowPaymentProvider` simulates hold/release/refund deterministically for tests
**Technical Notes**: Architecture ADR-2
**Dependencies**: 1.1

### Story 4.2: Create booking + escrow hold
**Priority**: Must | **Size**: L | **Specialist**: Backend Dev + Frontend Dev
**Acceptance Criteria**:
- [ ] Given a matched, verified tutor, when student confirms booking and payment succeeds, then booking status = ESCROW_HELD and `escrow_transactions` row created (server-computed price, never client-supplied — adversarial checklist)
- [ ] Given payment fails, when booking is attempted, then status stays PENDING_PAYMENT and no escrow row is created
**Technical Notes**: ATDD scenario "Student books a matched tutor"; DB `bookings`/`escrow_transactions`
**Dependencies**: 4.1, 3.4, 2.4

### Story 4.3: Mutual completion confirmation → escrow release
**Priority**: Must | **Size**: M | **Specialist**: Backend Dev + Frontend Dev
**Acceptance Criteria**:
- [ ] Given only one party confirms, when checked, then status remains ESCROW_HELD
- [ ] Given both parties confirm, when the second confirms, then status becomes COMPLETED and escrow RELEASED atomically (race-condition guarded per adversarial checklist)
**Dependencies**: 4.2

### Story 4.4: Dispute flow (admin-mediated)
**Priority**: Should | **Size**: M | **Specialist**: Backend Dev + Frontend Dev
**Acceptance Criteria**:
- [ ] Given either party reports an issue, when raised, then status becomes DISPUTED
- [ ] Given admin resolves a dispute, when they choose release or refund, then escrow status updates accordingly and audit log records it
**Dependencies**: 4.3

**Definition of Done (Sprint 4-5)**: Full booking→escrow→completion path works with mock CMI; disputes resolvable by admin. Maximum-rigor adversarial tests pass (idempotency, race conditions, IDOR, price tampering). Coverage ≥ 80%.

---

## Epic 5: Reviews & Messaging
### Story 5.1: Post-completion rating & review
**Priority**: Must | **Size**: S | **Specialist**: Backend Dev + Frontend Dev
**Acceptance Criteria**:
- [ ] Given a COMPLETED booking, when a party submits a rating (1-5) + comment, then a `reviews` row is created (one per party, enforced unique)
- [ ] Given a booking not yet completed, when review submission is attempted, then it's rejected
**Dependencies**: 4.3

### Story 5.2: In-app messaging per booking/gig thread
**Priority**: Must | **Size**: M | **Specialist**: Backend Dev + Frontend Dev
**Acceptance Criteria**:
- [ ] Given a booking or matched gig, when either party sends a message, then it appears in the shared thread for both, scoped so non-parties get 403
**Technical Notes**: FR-8; `message_threads`/`messages`
**Dependencies**: 4.2

**Definition of Done (Sprint 6)**: Reviews and messaging work end-to-end, scoped correctly. Coverage ≥ 80%.

---

## Epic 6: Notifications & Admin Analytics
### Story 6.1: Domain-event-driven notifications (in-app + email)
**Priority**: Must | **Size**: M | **Specialist**: Backend Dev
**Acceptance Criteria**:
- [ ] Given a `BookingCompletedEvent`/`EscrowReleasedEvent`/`TutorVerifiedEvent` fires, when handled, then an in-app `notifications` row is created and an email is sent (async, non-blocking)
**Technical Notes**: Architecture ADR-3
**Dependencies**: 4.3, 2.3

### Story 6.2: Notification center UI
**Priority**: Should | **Size**: S | **Specialist**: Frontend Dev
**Acceptance Criteria**:
- [ ] Given unread notifications exist, when the user opens the bell icon, then unread items are listed and can be marked read
**Dependencies**: 6.1

### Story 6.3: Admin analytics dashboard
**Priority**: Should | **Size**: M | **Specialist**: Backend Dev + Frontend Dev
**Acceptance Criteria**:
- [ ] Given admin views the dashboard, when loaded, then signups, bookings, GMV, and match rate are displayed (PRD success metrics)
**Dependencies**: 4.3, 3.3

**Definition of Done (Sprint 7)**: Notifications fire correctly across the domain events; admin has visibility into platform health metrics. Coverage ≥ 80%.

---

## Epic 7: i18n/RTL Polish, Hardening & Launch
### Story 7.1: Full FR/AR/EN translation pass
**Priority**: Must | **Size**: M | **Specialist**: Frontend Dev + Copywriter
**Acceptance Criteria**:
- [ ] Given every screen built in Sprints 1-7, when language is switched, then all UI text is translated and RTL renders correctly (no mirrored-but-untranslated gaps)
**Technical Notes**: NFR-4; Test Strategy i18n/RTL scenario
**Dependencies**: All prior frontend stories

### Story 7.2: Security hardening pass + dependency/secret scans clean
**Priority**: Must | **Size**: M | **Specialist**: Security Engineer + DevSecOps
**Acceptance Criteria**:
- [ ] Given the full adversarial checklist (Test Strategy §4), when run, then no critical/high findings remain open
- [ ] Given CI security scans (Semgrep/Trivy/Gitleaks), when run, then all pass
**Dependencies**: All prior backend stories

### Story 7.3: Switch CMI from mock to live (if credentials available)
**Priority**: Could | **Size**: M | **Specialist**: Backend Dev
**Acceptance Criteria**:
- [ ] Given real CMI sandbox/production credentials, when `CMI_MODE=live`, then `CmiEscrowPaymentProvider` handles real hold/release/refund calls
**Dependencies**: 4.1
**Note**: Marked Could — if CMI credentials aren't available by this sprint, ship with mock mode and revisit as a fast-follow; doesn't block launch of the rest of the platform per YAGNI.

### Story 7.4: Final E2E suite + video recording + Docker production deploy
**Priority**: Must | **Size**: L | **Specialist**: Test Architect + Deployment + DevOps
**Acceptance Criteria**:
- [ ] Given the full critical-path E2E suite (registration → verification → gig → match → booking → escrow → completion → review, in FR and AR), when run, then all pass
- [ ] Given this is the final sprint of v1, when E2E runs, then a Playwright video recording is captured and saved to `.recordings/v1-2026-MM-DD.webm` (first and only video recording per project convention — earlier sprints run E2E headless without recording)
- [ ] Given the release gate criteria (Test Strategy §5), when checked, then all pass
- [ ] Given production Docker Compose config, when deployed, then the app is live and reachable
**Dependencies**: All prior stories

**Definition of Done (Sprint 8 — v1 launch)**: Full trilingual platform hardened, tested end-to-end with recorded video, deployed via Docker. This is the project's first version completion milestone.

---

## Sprint Allocation
| Sprint | Epic(s) | Stories | Notes |
|---|---|---|---|
| Sprint 1 | Foundation & Auth | 1.1, 1.2, 1.3, 1.4 | Scaffold, CI, Docker, auth, i18n skeleton |
| Sprint 2 | Profiles & Verification | 2.1, 2.2, 2.3, 2.4 | Tutor verification workflow end-to-end |
| Sprint 3 | Gigs & Matching | 3.1, 3.2, 3.3, 3.4 | pgvector matching + fallback search |
| Sprint 4 | Booking & Escrow (part 1) | 4.1, 4.2 | Mock payment provider, booking creation + escrow hold |
| Sprint 5 | Booking & Escrow (part 2) | 4.3, 4.4 | Completion/release, dispute flow, adversarial hardening |
| Sprint 6 | Reviews & Messaging | 5.1, 5.2 | |
| Sprint 7 | Notifications & Admin | 6.1, 6.2, 6.3 | |
| Sprint 8 | i18n Polish, Hardening & Launch | 7.1, 7.2, 7.3, 7.4 | v1 launch — video recording happens here only |

## Traceability (sample — extend as stories complete)
| PRD Requirement | Architecture Decision | Story | Acceptance Test | Sprint |
|---|---|---|---|---|
| FR-5 (escrow pre-payment) | ADR-2 (Payment Strategy interface) | 4.2 | ATDD "Student books a matched tutor" | 4 |
| FR-6 (mutual completion release) | ADR-3 (domain events) | 4.3 | ATDD "Escrow releases only after both confirm" | 5 |
| FR-2 (verification) | Database `tutor_profiles`/`verification_documents` | 2.2, 2.3 | ATDD "Admin approves a tutor" | 2 |
| NFR-4 (RTL) | UI doc §5, Architecture ADR-4 | 1.4, 7.1 | ATDD "Arabic renders right-to-left" | 1, 8 |
