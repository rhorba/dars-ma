CREATE TABLE match_suggestions (
  id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  gig_request_id  UUID NOT NULL REFERENCES gig_requests(id) ON DELETE CASCADE,
  tutor_user_id   UUID NOT NULL REFERENCES tutor_profiles(user_id) ON DELETE CASCADE,
  similarity_score NUMERIC(5,4) NOT NULL,
  created_at      TIMESTAMPTZ NOT NULL DEFAULT NOW(),
  UNIQUE(gig_request_id, tutor_user_id)
);
CREATE INDEX idx_match_suggestions_gig ON match_suggestions(gig_request_id, similarity_score DESC);
