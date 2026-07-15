import { TestBed } from '@angular/core/testing';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { provideHttpClient } from '@angular/common/http';
import { BookingService } from './booking.service';
import { Booking } from './booking.models';

describe('BookingService', () => {
  let service: BookingService;
  let httpMock: HttpTestingController;

  const sampleBooking: Booking = {
    id: 'booking-1',
    gigRequestId: 'gig-1',
    studentUserId: 'student-1',
    tutorUserId: 'tutor-1',
    agreedPriceMad: 200,
    status: 'ESCROW_HELD',
    studentConfirmedAt: null,
    tutorConfirmedAt: null,
    createdAt: '2026-07-15T00:00:00Z'
  };

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [provideHttpClient(), provideHttpClientTesting()]
    });
    service = TestBed.inject(BookingService);
    httpMock = TestBed.inject(HttpTestingController);
  });

  afterEach(() => httpMock.verify());

  it('createBooking() POSTs the request body to /api/v1/bookings', () => {
    service
      .createBooking({ gigRequestId: 'gig-1', tutorUserId: 'tutor-1', durationHours: 2 })
      .subscribe((booking) => expect(booking).toEqual(sampleBooking));
    const req = httpMock.expectOne('/api/v1/bookings');
    expect(req.request.method).toBe('POST');
    expect(req.request.body).toEqual({ gigRequestId: 'gig-1', tutorUserId: 'tutor-1', durationHours: 2 });
    req.flush(sampleBooking);
  });

  it('getBooking() GETs /api/v1/bookings/:id', () => {
    service.getBooking('booking-1').subscribe((booking) => expect(booking).toEqual(sampleBooking));
    const req = httpMock.expectOne('/api/v1/bookings/booking-1');
    expect(req.request.method).toBe('GET');
    req.flush(sampleBooking);
  });

  it('completeBooking() POSTs to /api/v1/bookings/:id/complete', () => {
    service.completeBooking('booking-1').subscribe();
    const req = httpMock.expectOne('/api/v1/bookings/booking-1/complete');
    expect(req.request.method).toBe('POST');
    req.flush(sampleBooking);
  });

  it('disputeBooking() POSTs to /api/v1/bookings/:id/dispute', () => {
    service.disputeBooking('booking-1').subscribe();
    const req = httpMock.expectOne('/api/v1/bookings/booking-1/dispute');
    expect(req.request.method).toBe('POST');
    req.flush(sampleBooking);
  });
});
