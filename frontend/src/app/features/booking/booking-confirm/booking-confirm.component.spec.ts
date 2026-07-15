import { TestBed } from '@angular/core/testing';
import { ActivatedRoute, convertToParamMap, provideRouter, Router } from '@angular/router';
import { provideHttpClient } from '@angular/common/http';
import { provideHttpClientTesting } from '@angular/common/http/testing';
import { provideNoopAnimations } from '@angular/platform-browser/animations';
import { provideTranslateService } from '@ngx-translate/core';
import { vi } from 'vitest';
import { of, throwError } from 'rxjs';
import { BookingConfirmComponent } from './booking-confirm.component';
import { BookingService } from '../../../core/booking/booking.service';
import { ProfileService } from '../../../core/profile/profile.service';
import { Booking } from '../../../core/booking/booking.models';
import { TutorProfile } from '../../../core/profile/profile.models';

describe('BookingConfirmComponent', () => {
  let bookingServiceStub: { createBooking: ReturnType<typeof vi.fn> };
  let profileServiceStub: { getPublicTutorProfile: ReturnType<typeof vi.fn> };
  let router: Router;

  const tutor: TutorProfile = {
    userId: 'tutor-1',
    bio: 'Bio',
    subjects: ['Math'],
    hourlyRateMad: 100,
    verificationStatus: 'VERIFIED',
    avgRating: null
  };

  const booking: Booking = {
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
      imports: [BookingConfirmComponent],
      providers: [
        provideRouter([]),
        provideHttpClient(),
        provideHttpClientTesting(),
        provideNoopAnimations(),
        provideTranslateService({ fallbackLang: 'fr' }),
        { provide: BookingService, useValue: bookingServiceStub },
        { provide: ProfileService, useValue: profileServiceStub },
        {
          provide: ActivatedRoute,
          useValue: { snapshot: { paramMap: convertToParamMap({ gigId: 'gig-1', tutorUserId: 'tutor-1' }) } }
        }
      ]
    }).compileComponents();
    router = TestBed.inject(Router);
    vi.spyOn(router, 'navigate');
  }

  it('loads the tutor and computes an estimated price from duration x rate', async () => {
    bookingServiceStub = { createBooking: vi.fn() };
    profileServiceStub = { getPublicTutorProfile: vi.fn().mockReturnValue(of(tutor)) };
    await configure();
    const fixture = TestBed.createComponent(BookingConfirmComponent);
    fixture.detectChanges();

    expect(profileServiceStub.getPublicTutorProfile).toHaveBeenCalledWith('tutor-1');
    fixture.componentInstance.form.setValue({ durationHours: 2 });
    expect(fixture.componentInstance.estimatedPrice()).toBe(200);
  });

  it('submits and navigates to the booking detail page on success', async () => {
    bookingServiceStub = { createBooking: vi.fn().mockReturnValue(of(booking)) };
    profileServiceStub = { getPublicTutorProfile: vi.fn().mockReturnValue(of(tutor)) };
    await configure();
    const fixture = TestBed.createComponent(BookingConfirmComponent);
    fixture.detectChanges();
    fixture.componentInstance.form.setValue({ durationHours: 2 });

    fixture.componentInstance.submit();

    expect(bookingServiceStub.createBooking).toHaveBeenCalledWith({
      gigRequestId: 'gig-1',
      tutorUserId: 'tutor-1',
      durationHours: 2
    });
    expect(router.navigate).toHaveBeenCalledWith(['/bookings', 'booking-1']);
  });

  it('shows an error message when submission fails', async () => {
    bookingServiceStub = { createBooking: vi.fn().mockReturnValue(throwError(() => new Error('conflict'))) };
    profileServiceStub = { getPublicTutorProfile: vi.fn().mockReturnValue(of(tutor)) };
    await configure();
    const fixture = TestBed.createComponent(BookingConfirmComponent);
    fixture.detectChanges();
    fixture.componentInstance.form.setValue({ durationHours: 1 });

    fixture.componentInstance.submit();

    expect(fixture.componentInstance.errorMessageKey()).toBe('booking.confirm.submitError');
    expect(router.navigate).not.toHaveBeenCalled();
  });

  it('does not submit when the form is invalid', async () => {
    bookingServiceStub = { createBooking: vi.fn() };
    profileServiceStub = { getPublicTutorProfile: vi.fn().mockReturnValue(of(tutor)) };
    await configure();
    const fixture = TestBed.createComponent(BookingConfirmComponent);
    fixture.detectChanges();
    fixture.componentInstance.form.setValue({ durationHours: 20 });

    fixture.componentInstance.submit();

    expect(bookingServiceStub.createBooking).not.toHaveBeenCalled();
  });
});
