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

CREATE TABLE escrow_transactions (
  id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  booking_id      UUID NOT NULL UNIQUE REFERENCES bookings(id),
  cmi_reference   VARCHAR(255),
  amount_mad      NUMERIC(10,2) NOT NULL,
  status          VARCHAR(20) NOT NULL CHECK (status IN ('HELD','RELEASED','REFUNDED')),
  held_at         TIMESTAMPTZ,
  released_at     TIMESTAMPTZ,
  created_at      TIMESTAMPTZ NOT NULL DEFAULT NOW()
);
CREATE INDEX idx_escrow_transactions_booking ON escrow_transactions(booking_id);
