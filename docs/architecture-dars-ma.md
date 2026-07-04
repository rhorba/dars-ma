# Architecture: Dars.ma
**PRD Reference**: docs/prd-dars-ma.md
**System Design Reference**: docs/system-design-dars-ma.md
**Version**: 1.0 | **Date**: 2026-07-04 | **Author**: Software Architect

## 1. Overview
A layered, package-by-feature Spring Boot monolith (backend) + Angular SPA (frontend), talking over REST/JSON. No DDD/hexagonal/CQRS — the domain doesn't justify that complexity yet. One exception: the CMI payment gateway sits behind a `PaymentProvider` interface (Strategy pattern) so it can be swapped/mocked without touching booking logic.

## 2. Architecture Decision Records

### ADR-1: Layered, Package-by-Feature Monolith
- **Status**: Accepted
- **Context**: Modular monolith was chosen at system-design level. Need an internal structure that's maintainable by a small team without over-engineering.
- **Decision**: Simple layered architecture (Controller → Service → Repository → Entity) organized by feature package (`auth`, `profile`, `gig`, `matching`, `booking`, `messaging`, `notification`, `admin`), not by technical layer.
- **Consequences**:
  + Easy to find everything related to one feature; easy to extract a module into its own service later if needed
  - Some cross-feature coupling (e.g., booking needs profile + gig) is unavoidable — resolved via each feature exposing a thin service interface others call, never direct repository access across packages
- **Re-evaluate when**: A feature package's complexity clearly outgrows CRUD+service (candidate: `matching` or `booking/escrow`, revisit with DDD if so)

### ADR-2: Payment Gateway Behind a Strategy Interface
- **Status**: Accepted
- **Context**: CMI escrow integration is external, may be slow to get sandbox access to (see PRD risk), and must be mockable for dev/testing.
- **Decision**: Define `EscrowPaymentProvider` interface (`hold`, `release`, `refund`) in the `booking` package; `CmiEscrowPaymentProvider` is the real implementation, `MockEscrowPaymentProvider` backs local/dev/test via a Spring profile.
- **Consequences**:
  + Booking/escrow logic is fully testable without live CMI access
  - One extra interface layer, justified by real external-dependency risk
- **Re-evaluate when**: Never — this is a standing pattern for any external payment gateway

### ADR-3: Domain Events for Cross-Feature Side Effects
- **Status**: Accepted
- **Context**: Booking, matching, and notification features need to react to each other's state changes (e.g., booking completed → trigger rating eligibility + notification) without direct coupling.
- **Decision**: Use Spring's `ApplicationEventPublisher`/`@EventListener` (in-process, synchronous or `@Async`) for events like `BookingCompletedEvent`, `EscrowReleasedEvent`, `TutorVerifiedEvent`. No external message broker (per System Design YAGNI call).
- **Consequences**:
  + Decoupled features, easy to add new listeners (e.g., analytics) without touching the source feature
  - Events are in-process only — lost on crash mid-processing; acceptable since nothing here is financially final without a DB transaction completing first
- **Re-evaluate when**: Cross-service (not just cross-package) event delivery is needed

### ADR-4: Angular Standalone Components + Feature Modules by Route
- **Status**: Accepted
- **Context**: Angular latest LTS defaults to standalone components (no NgModules required). Need a structure that scales to ~8 feature areas without a heavyweight module tree.
- **Decision**: Feature folders under `src/app/features/{auth,gigs,tutors,booking,messaging,admin}`, each with standalone components + lazy-loaded routes. Shared UI in `src/app/shared`, cross-cutting services (auth interceptor, i18n) in `src/app/core`.
- **Consequences**:
  + Lazy loading per feature keeps initial bundle small; matches Angular LTS best practice
  - None significant
- **Re-evaluate when**: N/A

## 3. System Design
```
[Angular SPA] → [Nginx] → [Spring Boot Monolith]
                                 ├── auth          (JWT issue/verify)
                                 ├── profile        (student/tutor profiles, verification docs)
                                 ├── gig             (gig requests)
                                 ├── matching        (pgvector queries over profile+gig embeddings)
                                 ├── booking         (booking + escrow state machine)
                                 │      → EscrowPaymentProvider → [CMI Gateway]
                                 ├── messaging       (thread-scoped messages)
                                 ├── notification    (in-app + email, listens to domain events)
                                 └── admin           (verification review, disputes, analytics)
                                 ↓
                          [PostgreSQL + pgvector]
```

## 4. Data Model (overview — full schema owned by DBA)
```
User ──1:1──> Profile (Student | Tutor discriminated)
Profile(Tutor) ──1:N──> VerificationDocument
Profile(Tutor) ──1:N──> SkillEmbedding (pgvector)
User(Student) ──1:N──> GigRequest ──1:1──> GigEmbedding (pgvector)
GigRequest ──1:N──> MatchSuggestion ──> Profile(Tutor)
GigRequest ──1:1──> Booking ──1:1──> EscrowTransaction
Booking ──1:1──> Review (post-completion, one per side)
Booking ──1:N──> MessageThread ──1:N──> Message
User ──N:N──> Notification
```

## 5. API Design (representative — full contract grows with stories)
| Method | Endpoint | Description | Auth |
|---|---|---|---|
| POST | /api/v1/auth/register | Register student or tutor | Public |
| POST | /api/v1/auth/login | Login, issue JWT | Public |
| GET | /api/v1/profiles/tutors/:id | View tutor profile | Public |
| POST | /api/v1/profiles/tutors/:id/verification | Submit verification docs | Tutor (owner) |
| PATCH | /api/v1/admin/verifications/:id | Approve/reject verification | Admin |
| POST | /api/v1/gigs | Create gig request | Student |
| GET | /api/v1/gigs/:id/matches | Get pgvector-matched tutors | Student (owner) |
| POST | /api/v1/bookings | Create booking (triggers escrow hold) | Student |
| POST | /api/v1/bookings/:id/complete | Confirm session completion | Student or Tutor (party) |
| POST | /api/v1/bookings/:id/reviews | Submit review | Student or Tutor (party, post-completion) |
| GET | /api/v1/bookings/:id/messages | List thread messages | Party to booking |
| GET | /api/v1/admin/analytics | Dashboard metrics | Admin |

## 6. Security Considerations
[Full detail owned by Security Engineer — docs/security-dars-ma.md]
- Authentication: JWT (stateless, fits horizontal scaling per System Design)
- Authorization: Role-based (Student/Tutor/Admin) + resource-ownership checks (e.g., only booking parties see messages)
- Data protection: Verification documents and payment references are the highest-sensitivity PII — encrypted at rest
- Key risks: escrow tampering, verification document forgery, cross-tenant data leakage (booking/messages)

## 7. Infrastructure
- Hosting: Single Docker host (VPS), Docker Compose
- Database: PostgreSQL + pgvector, containerized, volume-mounted for persistence
- CI/CD: GitHub Actions (build, test, coverage gate, security scan, Docker image build)
- Monitoring: Container logs (stdout → Docker log driver); revisit if scale demands more

## 8. Technical Risks
| Risk | Mitigation | Owner |
|---|---|---|
| CMI integration delay blocks booking feature | `EscrowPaymentProvider` interface + mock implementation unblocks all dev/testing | Backend Dev |
| pgvector query performance at low data volume gives poor matches | Matching module falls back to filter search below a minimum tutor-count threshold | Backend Dev |
| Cross-feature coupling creeping into direct repository access | Enforce via code review + package-private repository visibility | Tech Lead |
| Angular RTL (Arabic) layout bugs | Bake RTL testing into Test Strategy from Sprint 1 | Frontend Dev / Test Architect |
