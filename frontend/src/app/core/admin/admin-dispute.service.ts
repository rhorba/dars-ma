import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { Booking } from '../booking/booking.models';

export type DisputeResolution = 'RELEASE' | 'REFUND';

@Injectable({ providedIn: 'root' })
export class AdminDisputeService {
  constructor(private http: HttpClient) {}

  getDisputedBookings(): Observable<Booking[]> {
    return this.http.get<Booking[]>('/api/v1/admin/bookings/disputes');
  }

  resolve(bookingId: string, resolution: DisputeResolution): Observable<Booking> {
    return this.http.post<Booking>(`/api/v1/admin/bookings/${bookingId}/disputes/resolve`, { resolution });
  }
}
