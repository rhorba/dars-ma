export interface Review {
  id: string;
  bookingId: string;
  reviewerId: string;
  rating: number;
  comment: string | null;
  createdAt: string;
}

export interface ReviewCreateRequest {
  rating: number;
  comment: string | null;
}
