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

CREATE TABLE message_threads (
  id             UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  booking_id     UUID UNIQUE REFERENCES bookings(id) ON DELETE CASCADE,
  gig_request_id UUID REFERENCES gig_requests(id) ON DELETE CASCADE,
  created_at     TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE TABLE messages (
  id          UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  thread_id   UUID NOT NULL REFERENCES message_threads(id) ON DELETE CASCADE,
  sender_id   UUID NOT NULL REFERENCES users(id),
  body        TEXT NOT NULL,
  created_at  TIMESTAMPTZ NOT NULL DEFAULT NOW()
);
CREATE INDEX idx_messages_thread ON messages(thread_id, created_at);
