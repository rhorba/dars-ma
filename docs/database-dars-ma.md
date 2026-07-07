# Database Design: Dars.ma
**Architecture Reference**: docs/architecture-dars-ma.md
**Version**: 1.0 | **Date**: 2026-07-04 | **Author**: DBA

## 1. Database Selection
- **Engine**: PostgreSQL 16+ with the `pgvector` extension
- **Rationale**: Structured relational data (users, bookings, escrow) plus vector similarity search (tutor/gig matching) — pgvector avoids running a second database system for embeddings. Volume is small (thousands of rows), read/write pattern is not extreme in either direction.
- **Hosting**: Self-hosted in Docker (per System Design), single instance, daily `pg_dump` backup

## 2. Entity-Relationship Model
```
users ──1:1──> profiles (discriminated: STUDENT | TUTOR)
profiles(TUTOR) ──1:N──> verification_documents
profiles(TUTOR) ──1:1──> tutor_embeddings (pgvector)
gig_requests ──1:1──> gig_embeddings (pgvector)
users(STUDENT) ──1:N──> gig_requests
gig_requests ──1:N──> match_suggestions ──N:1──> profiles(TUTOR)
gig_requests ──1:1──> bookings
bookings ──1:1──> escrow_transactions
bookings ──1:N──> reviews (max 2: one per party)
bookings ──1:1──> message_threads ──1:N──> messages
users ──1:N──> notifications
users(ADMIN implicit via role) ──1:N──> audit_log_entries (actor)
```

## 3. Schema Design

```sql
CREATE EXTENSION IF NOT EXISTS vector;
CREATE EXTENSION IF NOT EXISTS pgcrypto; -- gen_random_uuid()

-- Table: users
CREATE TABLE users (
  id             UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  email          VARCHAR(255) NOT NULL UNIQUE,
  password_hash  VARCHAR(255) NOT NULL,
  role           VARCHAR(20)  NOT NULL CHECK (role IN ('STUDENT','TUTOR','ADMIN')),
  full_name      VARCHAR(255) NOT NULL,
  phone          VARCHAR(30),
  preferred_lang VARCHAR(5)   NOT NULL DEFAULT 'fr' CHECK (preferred_lang IN ('fr','ar','en')),
  is_active      BOOLEAN      NOT NULL DEFAULT TRUE,
  created_at     TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
  updated_at     TIMESTAMPTZ  NOT NULL DEFAULT NOW()
);
CREATE INDEX idx_users_email ON users(email);
CREATE INDEX idx_users_role ON users(role);

-- Table: tutor_profiles (1:1 with users where role=TUTOR)
CREATE TABLE tutor_profiles (
  user_id           UUID PRIMARY KEY REFERENCES users(id) ON DELETE CASCADE,
  bio               TEXT,
  subjects          TEXT[] NOT NULL DEFAULT '{}',
  hourly_rate_mad   NUMERIC(10,2) NOT NULL CHECK (hourly_rate_mad > 0),
  verification_status VARCHAR(20) NOT NULL DEFAULT 'PENDING' CHECK (verification_status IN ('PENDING','VERIFIED','REJECTED')),
  avg_rating        NUMERIC(3,2),
  created_at        TIMESTAMPTZ NOT NULL DEFAULT NOW(),
  updated_at        TIMESTAMPTZ NOT NULL DEFAULT NOW()
);
CREATE INDEX idx_tutor_profiles_verification ON tutor_profiles(verification_status);
CREATE INDEX idx_tutor_profiles_subjects ON tutor_profiles USING GIN(subjects);

-- Table: verification_documents (encrypted content at application layer before insert)
CREATE TABLE verification_documents (
  id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  tutor_user_id   UUID NOT NULL REFERENCES tutor_profiles(user_id) ON DELETE CASCADE,
  doc_type        VARCHAR(30) NOT NULL CHECK (doc_type IN ('DIPLOMA','CERTIFICATE','ID')),
  encrypted_blob  BYTEA NOT NULL,          -- application-layer encrypted document
  reviewed_by     UUID REFERENCES users(id), -- admin who reviewed
  reviewed_at     TIMESTAMPTZ,
  created_at      TIMESTAMPTZ NOT NULL DEFAULT NOW()
);
CREATE INDEX idx_verification_documents_tutor ON verification_documents(tutor_user_id);

-- Table: tutor_embeddings (pgvector)
CREATE TABLE tutor_embeddings (
  tutor_user_id UUID PRIMARY KEY REFERENCES tutor_profiles(user_id) ON DELETE CASCADE,
  embedding     VECTOR(384) NOT NULL,   -- sentence-transformers/paraphrase-multilingual-MiniLM-L12-v2 (native 384-dim, FR/AR/EN)
  updated_at    TIMESTAMPTZ NOT NULL DEFAULT NOW()
);
CREATE INDEX idx_tutor_embeddings_ann ON tutor_embeddings USING hnsw (embedding vector_cosine_ops);

-- Table: gig_requests
CREATE TABLE gig_requests (
  id             UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  student_user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
  subject        VARCHAR(100) NOT NULL,
  level          VARCHAR(50) NOT NULL,
  description    TEXT NOT NULL,
  budget_min_mad NUMERIC(10,2),
  budget_max_mad NUMERIC(10,2),
  status         VARCHAR(20) NOT NULL DEFAULT 'OPEN' CHECK (status IN ('OPEN','MATCHED','CLOSED')),
  created_at     TIMESTAMPTZ NOT NULL DEFAULT NOW(),
  updated_at     TIMESTAMPTZ NOT NULL DEFAULT NOW()
);
CREATE INDEX idx_gig_requests_student ON gig_requests(student_user_id);
CREATE INDEX idx_gig_requests_status ON gig_requests(status);

-- Table: gig_embeddings (pgvector)
CREATE TABLE gig_embeddings (
  gig_request_id UUID PRIMARY KEY REFERENCES gig_requests(id) ON DELETE CASCADE,
  embedding      VECTOR(384) NOT NULL,
  updated_at     TIMESTAMPTZ NOT NULL DEFAULT NOW()
);
CREATE INDEX idx_gig_embeddings_ann ON gig_embeddings USING hnsw (embedding vector_cosine_ops);

-- Table: match_suggestions
CREATE TABLE match_suggestions (
  id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  gig_request_id  UUID NOT NULL REFERENCES gig_requests(id) ON DELETE CASCADE,
  tutor_user_id   UUID NOT NULL REFERENCES tutor_profiles(user_id) ON DELETE CASCADE,
  similarity_score NUMERIC(5,4) NOT NULL,
  created_at      TIMESTAMPTZ NOT NULL DEFAULT NOW(),
  UNIQUE(gig_request_id, tutor_user_id)
);
CREATE INDEX idx_match_suggestions_gig ON match_suggestions(gig_request_id, similarity_score DESC);

-- Table: bookings
CREATE TABLE bookings (
  id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  gig_request_id  UUID NOT NULL UNIQUE REFERENCES gig_requests(id),
  student_user_id UUID NOT NULL REFERENCES users(id),
  tutor_user_id   UUID NOT NULL REFERENCES tutor_profiles(user_id),
  agreed_price_mad NUMERIC(10,2) NOT NULL CHECK (agreed_price_mad > 0),
  status          VARCHAR(20) NOT NULL DEFAULT 'PENDING_PAYMENT'
                    CHECK (status IN ('PENDING_PAYMENT','ESCROW_HELD','COMPLETED_STUDENT','COMPLETED_TUTOR','COMPLETED','DISPUTED','REFUNDED')),
  student_confirmed_at TIMESTAMPTZ,
  tutor_confirmed_at   TIMESTAMPTZ,
  created_at      TIMESTAMPTZ NOT NULL DEFAULT NOW(),
  updated_at      TIMESTAMPTZ NOT NULL DEFAULT NOW()
);
CREATE INDEX idx_bookings_student ON bookings(student_user_id);
CREATE INDEX idx_bookings_tutor ON bookings(tutor_user_id);
CREATE INDEX idx_bookings_status ON bookings(status);

-- Table: escrow_transactions
CREATE TABLE escrow_transactions (
  id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  booking_id      UUID NOT NULL UNIQUE REFERENCES bookings(id),
  cmi_reference   VARCHAR(255),              -- external gateway reference, not raw card data
  amount_mad      NUMERIC(10,2) NOT NULL,
  status          VARCHAR(20) NOT NULL CHECK (status IN ('HELD','RELEASED','REFUNDED')),
  held_at         TIMESTAMPTZ,
  released_at     TIMESTAMPTZ,
  created_at      TIMESTAMPTZ NOT NULL DEFAULT NOW()
);
CREATE INDEX idx_escrow_transactions_booking ON escrow_transactions(booking_id);

-- Table: reviews
CREATE TABLE reviews (
  id           UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  booking_id   UUID NOT NULL REFERENCES bookings(id) ON DELETE CASCADE,
  reviewer_id  UUID NOT NULL REFERENCES users(id),
  rating       SMALLINT NOT NULL CHECK (rating BETWEEN 1 AND 5),
  comment      TEXT,
  created_at   TIMESTAMPTZ NOT NULL DEFAULT NOW(),
  UNIQUE(booking_id, reviewer_id)
);
CREATE INDEX idx_reviews_booking ON reviews(booking_id);

-- Table: message_threads
CREATE TABLE message_threads (
  id          UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  booking_id  UUID UNIQUE REFERENCES bookings(id) ON DELETE CASCADE,
  gig_request_id UUID REFERENCES gig_requests(id) ON DELETE CASCADE,
  created_at  TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

-- Table: messages
CREATE TABLE messages (
  id          UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  thread_id   UUID NOT NULL REFERENCES message_threads(id) ON DELETE CASCADE,
  sender_id   UUID NOT NULL REFERENCES users(id),
  body        TEXT NOT NULL,
  created_at  TIMESTAMPTZ NOT NULL DEFAULT NOW()
);
CREATE INDEX idx_messages_thread ON messages(thread_id, created_at);

-- Table: notifications
CREATE TABLE notifications (
  id          UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  user_id     UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
  type        VARCHAR(50) NOT NULL,
  payload     JSONB NOT NULL DEFAULT '{}',
  read_at     TIMESTAMPTZ,
  created_at  TIMESTAMPTZ NOT NULL DEFAULT NOW()
);
CREATE INDEX idx_notifications_user_unread ON notifications(user_id) WHERE read_at IS NULL;

-- Table: audit_log_entries
CREATE TABLE audit_log_entries (
  id          UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  actor_id    UUID REFERENCES users(id),
  action      VARCHAR(100) NOT NULL,     -- e.g. 'VERIFICATION_APPROVED', 'ESCROW_RELEASED'
  target_type VARCHAR(50) NOT NULL,
  target_id   UUID NOT NULL,
  metadata    JSONB NOT NULL DEFAULT '{}',
  created_at  TIMESTAMPTZ NOT NULL DEFAULT NOW()
);
CREATE INDEX idx_audit_log_target ON audit_log_entries(target_type, target_id);
```

## 4. Index Strategy
| Table | Index Name | Columns | Query Pattern |
|---|---|---|---|
| users | idx_users_email | email | Login lookup |
| tutor_profiles | idx_tutor_profiles_subjects | subjects (GIN) | Filter search by subject |
| tutor_embeddings | idx_tutor_embeddings_ann | embedding (HNSW) | pgvector similarity search |
| gig_embeddings | idx_gig_embeddings_ann | embedding (HNSW) | pgvector similarity search |
| match_suggestions | idx_match_suggestions_gig | gig_request_id, similarity_score | Top-N matches per gig |
| bookings | idx_bookings_status | status | Admin dashboard, state-machine queries |
| notifications | idx_notifications_user_unread | user_id (partial: unread) | Unread notification badge/count |

## 5. Migration Plan
| Migration File | Description | Reversible |
|---|---|---|
| 001_extensions.sql | Enable `vector`, `pgcrypto` | Yes |
| 002_users_profiles.sql | `users`, `tutor_profiles`, `verification_documents` | Yes |
| 003_embeddings.sql | `tutor_embeddings`, `gig_embeddings` + HNSW indexes | Yes |
| 004_gigs_matching.sql | `gig_requests`, `match_suggestions` | Yes |
| 005_booking_escrow.sql | `bookings`, `escrow_transactions` | Yes |
| 006_reviews_messaging.sql | `reviews`, `message_threads`, `messages` | Yes |
| 007_notifications_audit.sql | `notifications`, `audit_log_entries` | Yes |

Managed via Flyway (standard Spring Boot migration tool) — each file forward-only in prod, `DROP TABLE IF EXISTS` counterparts kept in local rollback scripts for dev only.

## 6. Access Patterns
| Use Case | Query Pattern | Index Coverage |
|---|---|---|
| Login | SELECT by email | idx_users_email |
| Browse tutors by subject | SELECT WHERE subjects && ARRAY[...] | idx_tutor_profiles_subjects (GIN) |
| Get matches for a gig | pgvector cosine similarity ORDER BY embedding <=> :query LIMIT N | idx_tutor_embeddings_ann |
| Admin verification queue | SELECT WHERE verification_status='PENDING' | idx_tutor_profiles_verification |
| Booking dashboard | SELECT WHERE status IN (...) | idx_bookings_status |

## 7. Sensitive Data
- Columns requiring encryption: `verification_documents.encrypted_blob` (application-layer AES before insert), `escrow_transactions.cmi_reference` (DB-level column encryption or restrict via row-level access — no raw card data ever stored, CMI is PCI scope owner)
- Row-level security: not needed at this scale — enforced instead at the application layer via ownership checks (per Security Baseline); revisit RLS if direct DB access from multiple services is ever introduced
