# System Design: Dars.ma
**PRD Reference**: docs/prd-dars-ma.md
**Version**: 1.0 | **Date**: 2026-07-04 | **Author**: System Designer

## 1. Non-Functional Requirements
| Attribute | Target | Notes |
|---|---|---|
| Availability | 99% (87.6 hrs/yr downtime) | Single instance + restart. Upgrade to 99.9% only if paid tutoring bookings depend on 24/7 uptime post-launch. |
| Latency (p99) | < 500ms | Matches PRD NFR-1 (p95 < 500ms under ~500 concurrent users) |
| Throughput | ~500 concurrent users, ~50 RPS peak | DAU estimate: a few thousand at launch (Moroccan university market), req/user/day low (browse/book, not high-frequency) |
| Data Volume | < 1 GB/day | Text-heavy (profiles, messages, gig posts) + embeddings (pgvector, ~1-2KB/vector). No media/video storage in v1. |
| Retention | Indefinite for bookings/reviews (trust record); messages 1 year hot | No regulatory retention requirement identified yet |
| Recovery (RTO) | 60 min | Acceptable for a single-region MVP; restore from latest DB backup + redeploy containers |
| Recovery (RPO) | 24 hr | Nightly pg_dump backup is sufficient at this scale |

## 2. Component Topology

```
[Clients: Web (Angular SPA), mobile browsers]
        ↓ HTTPS
[Reverse Proxy: Nginx/Traefik]  ←── TLS termination, static Angular build serving
        ↓
[Spring Boot API (monolith, modular)]
  ├── Auth module        → [PostgreSQL]
  ├── Profile/Gig module → [PostgreSQL]
  ├── Matching module     → [PostgreSQL + pgvector]
  ├── Booking/Escrow module → [PostgreSQL] + [CMI Payment Gateway] (webhook callback)
  ├── Messaging module   → [PostgreSQL]
  ├── Notification module → [SMTP relay] (email) + in-app (DB-backed, polled/websocket later if needed)
  └── Admin module        → [PostgreSQL]
        ↓
[Observability: structured logs → stdout → Docker log driver]
```

**YAGNI notes**:
- No API gateway, load balancer, or CDN — single Docker host serves everything behind Nginx. Add a real LB only if traffic outgrows one host.
- No message queue — booking/escrow/notification events handled synchronously or via simple `@Async` in Spring Boot. Introduce a queue (e.g., RabbitMQ) only if async volume or reliability needs (retry, backpressure) prove it out.
- No Redis cache at launch — Postgres + proper indexing (see DBA doc) is enough at this scale. Add caching only if measured latency requires it.
- No microservices — one Spring Boot monolith with clear module boundaries (see Architecture doc), so it can be split later if a module's load genuinely outgrows the rest.

## 3. Integration Patterns
| Integration | Pattern | Reason |
|---|---|---|
| CMI Payment Gateway | REST + Webhook | Synchronous call to initiate escrow hold; async webhook confirms payment/capture/release events |
| Email (notifications) | SMTP (async, `@Async`) | Fire-and-forget; failure shouldn't block the triggering request |
| pgvector matching | In-process query (pgvector extension in same Postgres) | Avoids a separate vector DB/service — Postgres handles both relational + vector data at this scale |
| Angular ↔ Spring Boot | REST/JSON over HTTPS | Standard SPA-to-API pattern, no need for GraphQL/BFF at this scope |

## 4. Scalability Strategy
- Scaling approach: vertical (bigger Docker host) until proven insufficient; horizontal scaling of the Spring Boot container is possible later since it's stateless (sessions via JWT, not server-side session state)
- Cache strategy: none at launch; revisit if p99 latency exceeds target under real load
- Queue strategy: none at launch; revisit if escrow/notification volume needs retry/backpressure guarantees

## 5. System Design Decision Records

### SDR-1: Monolith vs. Microservices
- **NFR Driver**: Throughput (~50 RPS peak), team size (small)
- **Options**: 🟢 Modular monolith | 🟡 Monolith + separate matching service | 🔴 Full microservices
- **Decision**: 🟢 Modular monolith (single Spring Boot app, package-per-module boundaries enforced by Software Architect)
- **Trade-offs**: Less independent deployability per module; acceptable since team/scale don't need it
- **Re-evaluate when**: Any single module's load or team ownership genuinely outgrows shared deployment

### SDR-2: Deployment Target
- **NFR Driver**: Availability 99%, constraint = Docker-only per project mandate
- **Options**: 🟢 Docker Compose (single host) | 🟡 Docker Swarm (multi-host, simple orchestration) | 🔴 Kubernetes
- **Decision**: 🟢 Docker Compose. Kubernetes explicitly deferred — no current requirement for auto-scaling, multi-node scheduling, or rolling zero-downtime deploys at launch scale
- **Trade-offs**: Manual scaling/restart if the host fails; acceptable for 99% SLA target
- **Re-evaluate when**: Traffic requires multi-host scaling, or zero-downtime deploys become a hard requirement

### SDR-3: Vector Matching Storage
- **NFR Driver**: Data volume (small), FR-4 (pgvector matching)
- **Options**: 🟢 pgvector extension in main Postgres | 🟡 Separate vector DB (Pinecone/Weaviate) | 🔴 Custom ANN index service
- **Decision**: 🟢 pgvector in the same PostgreSQL instance — one database to operate, transactional consistency with relational data
- **Trade-offs**: pgvector ANN performance is fine at this candidate-pool size (hundreds-thousands of tutors), not built for tens of millions of vectors
- **Re-evaluate when**: Tutor count grows by orders of magnitude and query latency degrades
