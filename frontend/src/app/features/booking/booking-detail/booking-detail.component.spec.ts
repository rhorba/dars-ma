import { TestBed } from '@angular/core/testing';
import { ActivatedRoute, convertToParamMap } from '@angular/router';
import { provideHttpClient } from '@angular/common/http';
import { provideHttpClientTesting } from '@angular/common/http/testing';
import { provideTranslateService } from '@ngx-translate/core';
import { vi } from 'vitest';
import { of, throwError } from 'rxjs';
import { BookingDetailComponent } from './booking-detail.component';
import { BookingService } from '../../../core/booking/booking.service';
import { AuthService } from '../../../core/auth/auth.service';
import { Booking } from '../../../core/booking/booking.models';

describe('BookingDetailComponent', () => {
  let bookingServiceStub: { getBooking: ReturnType<typeof vi.fn>; completeBooking: ReturnType<typeof vi.fn> };
  let authServiceStub: { userId: ReturnType<typeof vi.fn> };

  const escrowHeldBooking: Booking = {
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

  async function configure() {
    await TestBed.configureTestingModule({
      imports: [BookingDetailComponent],
      providers: [
        provideHttpClient(),
        provideHttpClientTesting(),
        provideTranslateService({ fallbackLang: 'fr' }),
        { provide: BookingService, useValue: bookingServiceStub },
        { provide: AuthService, useValue: authServiceStub },
        { provide: ActivatedRoute, useValue: { snapshot: { paramMap: convertToParamMap({ id: 'booking-1' }) } } }
      ]
    }).compileComponents();
  }

  it('loads the booking and shows the mark-complete button for the student when unconfirmed', async () => {
    bookingServiceStub = { getBooking: vi.fn().mockReturnValue(of(escrowHeldBooking)), completeBooking: vi.fn() };
    authServiceStub = { userId: vi.fn().mockReturnValue('student-1') };
    await configure();
    const fixture = TestBed.createComponent(BookingDetailComponent);
    fixture.detectChanges();

    expect(fixture.componentInstance.booking()).toEqual(escrowHeldBooking);
    expect(fixture.componentInstance.canConfirm(escrowHeldBooking)).toBe(true);
  });

  it('hides the mark-complete button once the current party already confirmed', async () => {
    const alreadyConfirmed = { ...escrowHeldBooking, studentConfirmedAt: '2026-07-15T01:00:00Z' };
    bookingServiceStub = { getBooking: vi.fn().mockReturnValue(of(alreadyConfirmed)), completeBooking: vi.fn() };
    authServiceStub = { userId: vi.fn().mockReturnValue('student-1') };
    await configure();
    const fixture = TestBed.createComponent(BookingDetailComponent);
    fixture.detectChanges();

    expect(fixture.componentInstance.canConfirm(alreadyConfirmed)).toBe(false);
  });

  it('does not show the mark-complete button for a non-party', async () => {
    bookingServiceStub = { getBooking: vi.fn().mockReturnValue(of(escrowHeldBooking)), completeBooking: vi.fn() };
    authServiceStub = { userId: vi.fn().mockReturnValue('someone-else') };
    await configure();
    const fixture = TestBed.createComponent(BookingDetailComponent);
    fixture.detectChanges();

    expect(fixture.componentInstance.canConfirm(escrowHeldBooking)).toBe(false);
  });

  it('confirm() calls completeBooking and updates the booking signal', async () => {
    const completed = { ...escrowHeldBooking, status: 'COMPLETED' as const, studentConfirmedAt: 'x', tutorConfirmedAt: 'y' };
    bookingServiceStub = {
      getBooking: vi.fn().mockReturnValue(of(escrowHeldBooking)),
      completeBooking: vi.fn().mockReturnValue(of(completed))
    };
    authServiceStub = { userId: vi.fn().mockReturnValue('tutor-1') };
    await configure();
    const fixture = TestBed.createComponent(BookingDetailComponent);
    fixture.detectChanges();

    fixture.componentInstance.confirm();

    expect(bookingServiceStub.completeBooking).toHaveBeenCalledWith('booking-1');
    expect(fixture.componentInstance.booking()).toEqual(completed);
    expect(fixture.componentInstance.confirming()).toBe(false);
  });

  it('sets the error state when the booking fails to load', async () => {
    bookingServiceStub = { getBooking: vi.fn().mockReturnValue(throwError(() => new Error('403'))), completeBooking: vi.fn() };
    authServiceStub = { userId: vi.fn().mockReturnValue('someone') };
    await configure();
    const fixture = TestBed.createComponent(BookingDetailComponent);
    fixture.detectChanges();

    expect(fixture.componentInstance.error()).toBe(true);
  });
});
