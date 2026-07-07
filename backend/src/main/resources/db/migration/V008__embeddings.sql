CREATE TABLE tutor_embeddings (
  tutor_user_id UUID PRIMARY KEY REFERENCES tutor_profiles(user_id) ON DELETE CASCADE,
  embedding     VECTOR(384) NOT NULL,
  updated_at    TIMESTAMPTZ NOT NULL DEFAULT NOW()
);
CREATE INDEX idx_tutor_embeddings_ann ON tutor_embeddings USING hnsw (embedding vector_cosine_ops);

CREATE TABLE gig_embeddings (
  gig_request_id UUID PRIMARY KEY REFERENCES gig_requests(id) ON DELETE CASCADE,
  embedding      VECTOR(384) NOT NULL,
  updated_at     TIMESTAMPTZ NOT NULL DEFAULT NOW()
);
CREATE INDEX idx_gig_embeddings_ann ON gig_embeddings USING hnsw (embedding vector_cosine_ops);
