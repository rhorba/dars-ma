# DECISIONS — Dars.ma



## DECISION — 2026-07-04
Scope: COMPREHENSIVE (full README vision) — trilingual FR/AR/EN, escrow, pgvector matching, admin moderation, skill verification, notifications, analytics.
Sprint count target: 7-10 sprints, Scrum Master to size backlog against this.

## DECISION — 2026-07-04
Architecture: layered package-by-feature Spring Boot monolith. Payment gateway (CMI) behind EscrowPaymentProvider Strategy interface w/ mock impl. Spring in-process domain events for cross-feature reactions. Angular standalone components, lazy-loaded per feature.

## DECISION — 2026-07-04
Java toolchain gap (host has JDK21 max, no Maven; project targets Java 25): backend build/test runs via Docker containers only (maven:3.9-eclipse-temurin-25), no local JDK25/Maven install. Matches Docker-only project philosophy.

## Sprint 3 — Embedding generation approach (2026-07-06, session 4)
Decision: local multilingual sentence-transformer model (e.g. paraphrase-multilingual-MiniLM-L12-v2, native 384-dim, covers FR/AR/EN) run in-process via ONNX/DJL, no external API.
Why: matches project's Docker-only/self-contained pattern, avoids per-call cost and sending user bios/descriptions to a third party, and the DB schema already fixed VECTOR(384) which this model matches natively without dimension reduction.
Rejected: hashing-trick vector (too weak semantically, especially cross-language) and external API (cost, privacy, network dependency, doesn't fit no-K8s/self-contained ethos).
Model download strategy: download at first container startup into a cached Docker volume rather than baking into the image or committing to git (keeps repo/image small).

## Sprint 7 scope decision (2026-07-17, session 7)
REVIEW_RECEIVED will be retrofitted into Sprint 7's domain-event-driven notification mechanism (new ReviewSubmittedEvent), rather than left as a raw NotificationService.create() call. User-picked over leaving it as-is, for consistency with the new BookingCompletedEvent/EscrowReleasedEvent/TutorVerifiedEvent pattern.

## DECISION — Sprint 8 approach (2026-07-18, session 8)
Story 7.2 (security hardening) approach: confirmation pass, not a fresh rewrite. Audit Test Strategy §4 adversarial checklist line-by-line against existing tests (Sprints 1-5 already cover auth tamper/reuse/privilege-escalation, verification-upload MIME/path-traversal, booking/escrow maximum-rigor concurrency); re-run those suites + CI scans (Semgrep/Trivy/Gitleaks) to confirm still green; write new tests only for genuine gaps found during the audit. CMI webhook-replay checklist item is moot (no webhook endpoint exists since CMI stays mock).
Reason: avoids re-proving already-proven coverage (YAGNI) while still guaranteeing every checklist line traces to a real, currently-passing test before v1 ships.
