import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { Booking, BookingCreateRequest } from './booking.models';

@Injectable({ providedIn: 'root' })
export class BookingService {
  constructor(private http: HttpClient) {}

  createBooking(request: BookingCreateRequest): Observable<Booking> {
    return this.http.post<Booking>('/api/v1/bookings', request);
  }

  getBooking(id: string): Observable<Booking> {
    return this.http.get<Booking>(`/api/v1/bookings/${id}`);
  }

  completeBooking(id: string): Observable<Booking> {
    return this.http.post<Booking>(`/api/v1/bookings/${id}/complete`, {});
  }

  disputeBooking(id: string): Observable<Booking> {
    return this.http.post<Booking>(`/api/v1/bookings/${id}/dispute`, {});
  }
}
