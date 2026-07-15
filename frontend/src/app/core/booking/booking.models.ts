export type BookingStatus =
  | 'PENDING_PAYMENT'
  | 'ESCROW_HELD'
  | 'COMPLETED_STUDENT'
  | 'COMPLETED_TUTOR'
  | 'COMPLETED'
  | 'DISPUTED'
  | 'REFUNDED';

export interface Booking {
  id: string;
  gigRequestId: string;
  studentUserId: string;
  tutorUserId: string;
  agreedPriceMad: number;
  status: BookingStatus;
  studentConfirmedAt: string | null;
  tutorConfirmedAt: string | null;
  createdAt: string;
}

export interface BookingCreateRequest {
  gigRequestId: string;
  tutorUserId: string;
  durationHours: number;
}
