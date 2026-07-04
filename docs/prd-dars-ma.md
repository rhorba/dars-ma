# PRD: Dars.ma
**Version**: 1.0 | **Date**: 2026-07-04 | **Author**: PM | **Status**: Draft

## 1. Problem Statement
Moroccan university students find tutors through word-of-mouth with no way to verify tutor skill, no rating history, and no payment safety net. Tutors have no portfolio or way to build reputation. There is no platform that combines verified skills, escrow-protected payment, and smart matching for academic tutoring.

## 2. Goals & Success Metrics
| Goal | Metric | Target |
|---|---|---|
| Verified tutor supply | # tutors passing skill verification | 50 within first 3 months post-launch |
| Trust in payments | % of bookings using escrow (not cash) | 100% (escrow mandatory, no cash toggle) |
| Successful matches | % of gig posts matched to a tutor within 48h | ≥ 70% |
| Platform trust | Avg. tutor rating after 20 completed sessions | ≥ 4.2 / 5 |
| Language reach | % of sessions in AR or EN (non-FR) | ≥ 20% |

## 3. User Stories
As a **student**, I want to post what I need tutoring in, so that tutors can find and apply to help me.
As a **student**, I want to search/browse tutors by subject and see their verified skills and ratings, so that I can choose confidently.
As a **student**, I want to pay into escrow when I book, so that my money is only released once the session is confirmed complete.
As a **tutor**, I want to build a profile with verified skills and reviews, so that I can attract students.
As a **tutor**, I want to get matched to relevant gig requests, so that I don't have to cold-search for work.
As a **tutor**, I want escrow to guarantee I get paid once I deliver the session, so that I'm protected from non-payment.
As an **admin**, I want to moderate tutor verification submissions and flagged content, so that the platform stays trustworthy.
As any **user**, I want to use the platform in French, Arabic, or English, so that I can work in my preferred language.

- [ ] Story 1: Student posts a tutoring request (subject, level, budget, availability)
- [ ] Story 2: Tutor creates profile + submits skill verification (diploma/certificate upload or test)
- [ ] Story 3: Matching engine (pgvector) suggests tutors for a request / requests for a tutor
- [ ] Story 4: Booking flow with escrow payment via CMI
- [ ] Story 5: Session completion confirmation (both sides) releases escrow
- [ ] Story 6: Rating & review after session completion
- [ ] Story 7: In-app messaging between student and tutor (pre- and post-match)
- [ ] Story 8: Admin moderation dashboard (verify tutors, handle disputes/flags)
- [ ] Story 9: Notifications (in-app + email) for match, booking, payment, session events
- [ ] Story 10: i18n — FR/AR/EN including RTL layout support for Arabic
- [ ] Story 11: Basic analytics dashboard for admin (signups, bookings, GMV, match rate)

## 4. Scope
### In Scope
- Student, Tutor, Admin roles with role-based access
- Tutor skill verification workflow (document upload + admin review)
- Gig posting & browsing with keyword + pgvector semantic matching
- Escrow payment integration (CMI) — hold on booking, release on completion confirmation
- Ratings & reviews, tied to completed sessions only
- In-app messaging (text only)
- Admin moderation dashboard + basic analytics
- Notifications: in-app + email
- i18n: French, Arabic (RTL), English
- Web-responsive UI (desktop + mobile browser), Docker-only deployment

### Out of Scope (v1)
- Native mobile apps (iOS/Android)
- Live video/audio tutoring inside the platform (sessions happen off-platform; platform handles discovery, booking, payment, trust)
- Group tutoring / cohort classes
- Multi-currency (MAD only)
- Kubernetes (Docker Compose is the default; K8s only if a real scaling trigger appears — see System Design)

## 5. Requirements
### Functional
- FR-1: Users register/login as Student or Tutor; Admin accounts are provisioned, not self-registered
- FR-2: Tutors submit verification documents; status is Pending / Verified / Rejected
- FR-3: Students create gig requests with subject, level, description, budget range, availability
- FR-4: System suggests matches using pgvector similarity over tutor-subject embeddings + filters (price, availability, level)
- FR-5: Booking requires escrow pre-payment before a session is confirmed
- FR-6: Escrow releases to tutor only after both parties confirm session completion (or admin resolves a dispute)
- FR-7: Either party can rate/review only after a session is marked complete
- FR-8: Messaging is scoped to a match/booking thread
- FR-9: Admin can approve/reject tutor verification, view/resolve disputes, view analytics
- FR-10: All user-facing text is translatable; Arabic renders RTL
- FR-11: Notifications fire on: match found, booking created, payment held, session completed, escrow released, dispute opened

### Non-Functional
- NFR-1: Performance — p95 API response < 500ms under expected launch load (~500 concurrent users)
- NFR-2: Security — escrow and PII (national ID/diploma docs for verification) encrypted at rest; HTTPS everywhere
- NFR-3: Accessibility — WCAG 2.1 AA on core flows (post gig, book, pay, rate)
- NFR-4: Localization — full RTL support for Arabic, not just mirrored text
- NFR-5: Availability — single-region Docker deployment, target 99% uptime (no HA requirement at this stage)

## 6. Constraints & Assumptions
- Backend: Java 25 (LTS) + Spring Boot 3.x + Maven
- Frontend: Angular (latest LTS)
- Database: PostgreSQL + pgvector extension
- Deployment: Docker / Docker Compose only; Kubernetes deferred until a real scaling need is demonstrated
- Payment: CMI escrow integration (Morocco-specific payment gateway) — assumes merchant account exists or will be sandboxed for dev
- Assumption: tutor verification is manual admin review in v1 (no automated document-authenticity checking)

## 7. Risks
| Risk | Probability | Impact | Mitigation |
|---|---|---|---|
| CMI escrow integration complexity/sandbox access delays | M | H | Build payment as an isolated module behind an interface; stub/mock CMI in dev until sandbox creds arrive |
| pgvector matching quality is poor with low initial tutor supply | H | M | Fall back to filter-based search when candidate pool is small; don't block launch on matching quality |
| Arabic RTL bugs across Angular components | M | M | Bake RTL into UI Foundation + Test Strategy from sprint 1, not bolted on later |
| Scope (comprehensive) causes schedule slip across 7-10 sprints | M | M | Strict backlog prioritization (Must/Should/Could), YAGNI enforced per sprint review |

## 8. Timeline
| Milestone | Target Date |
|---|---|
| PRD Approved | 2026-07-04 |
| Foundation docs (all 10) approved | 2026-07-04 (session 1) |
| Sprint 1 start | Session 2 |
| End-to-end paid session milestone | ~Sprint 4-5 |
| v1 (comprehensive scope) complete | ~Sprint 7-10 |
