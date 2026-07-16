import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { Review, ReviewCreateRequest } from './review.models';

@Injectable({ providedIn: 'root' })
export class ReviewService {
  constructor(private http: HttpClient) {}

  submitReview(bookingId: string, request: ReviewCreateRequest): Observable<Review> {
    return this.http.post<Review>(`/api/v1/bookings/${bookingId}/reviews`, request);
  }

  getReviews(bookingId: string): Observable<Review[]> {
    return this.http.get<Review[]>(`/api/v1/bookings/${bookingId}/reviews`);
  }
}
