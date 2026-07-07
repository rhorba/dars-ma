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
