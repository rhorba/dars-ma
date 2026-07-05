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

CREATE TABLE verification_documents (
  id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  tutor_user_id   UUID NOT NULL REFERENCES tutor_profiles(user_id) ON DELETE CASCADE,
  doc_type        VARCHAR(30) NOT NULL CHECK (doc_type IN ('DIPLOMA','CERTIFICATE','ID')),
  encrypted_blob  BYTEA NOT NULL,
  reviewed_by     UUID REFERENCES users(id),
  reviewed_at     TIMESTAMPTZ,
  created_at      TIMESTAMPTZ NOT NULL DEFAULT NOW()
);
CREATE INDEX idx_verification_documents_tutor ON verification_documents(tutor_user_id);
