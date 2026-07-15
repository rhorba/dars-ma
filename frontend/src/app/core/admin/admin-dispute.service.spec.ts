import { TestBed } from '@angular/core/testing';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { provideHttpClient } from '@angular/common/http';
import { AdminDisputeService } from './admin-dispute.service';
import { Booking } from '../booking/booking.models';

describe('AdminDisputeService', () => {
  let service: AdminDisputeService;
  let httpMock: HttpTestingController;

  const disputedBooking: Booking = {
    id: 'booking-1',
    gigRequestId: 'gig-1',
    studentUserId: 'student-1',
    tutorUserId: 'tutor-1',
    agreedPriceMad: 200,
    status: 'DISPUTED',
    studentConfirmedAt: null,
    tutorConfirmedAt: null,
    createdAt: '2026-07-15T00:00:00Z'
  };

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [provideHttpClient(), provideHttpClientTesting()]
    });
    service = TestBed.inject(AdminDisputeService);
    httpMock = TestBed.inject(HttpTestingController);
  });

  afterEach(() => httpMock.verify());

  it('getDisputedBookings() GETs /api/v1/admin/bookings/disputes', () => {
    service.getDisputedBookings().subscribe((bookings) => expect(bookings).toEqual([disputedBooking]));
    const req = httpMock.expectOne('/api/v1/admin/bookings/disputes');
    expect(req.request.method).toBe('GET');
    req.flush([disputedBooking]);
  });

  it('resolve() POSTs the resolution to /api/v1/admin/bookings/:id/disputes/resolve', () => {
    service.resolve('booking-1', 'RELEASE').subscribe();
    const req = httpMock.expectOne('/api/v1/admin/bookings/booking-1/disputes/resolve');
    expect(req.request.method).toBe('POST');
    expect(req.request.body).toEqual({ resolution: 'RELEASE' });
    req.flush(disputedBooking);
  });
});
