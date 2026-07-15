import { TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { provideHttpClientTesting } from '@angular/common/http/testing';
import { provideNoopAnimations } from '@angular/platform-browser/animations';
import { provideTranslateService } from '@ngx-translate/core';
import { vi } from 'vitest';
import { of, throwError } from 'rxjs';
import { DisputeQueueComponent } from './dispute-queue.component';
import { AdminDisputeService } from '../../../core/admin/admin-dispute.service';
import { Booking } from '../../../core/booking/booking.models';

describe('DisputeQueueComponent', () => {
  let adminDisputeServiceStub: {
    getDisputedBookings: ReturnType<typeof vi.fn>;
    resolve: ReturnType<typeof vi.fn>;
  };

  const booking: Booking = {
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

  beforeEach(async () => {
    adminDisputeServiceStub = {
      getDisputedBookings: vi.fn().mockReturnValue(of([booking])),
      resolve: vi.fn()
    };

    await TestBed.configureTestingModule({
      imports: [DisputeQueueComponent],
      providers: [
        provideHttpClient(),
        provideHttpClientTesting(),
        provideNoopAnimations(),
        provideTranslateService({ fallbackLang: 'fr' }),
        { provide: AdminDisputeService, useValue: adminDisputeServiceStub }
      ]
    }).compileComponents();
  });

  it('loads the disputed bookings queue on init', () => {
    const fixture = TestBed.createComponent(DisputeQueueComponent);
    fixture.detectChanges();

    expect(fixture.componentInstance.bookings()).toEqual([booking]);
  });

  it('resolve() with RELEASE reloads the queue and sets a success message', () => {
    adminDisputeServiceStub.resolve.mockReturnValue(of(booking));
    const fixture = TestBed.createComponent(DisputeQueueComponent);
    fixture.detectChanges();

    fixture.componentInstance.resolve('booking-1', 'RELEASE');

    expect(adminDisputeServiceStub.resolve).toHaveBeenCalledWith('booking-1', 'RELEASE');
    expect(fixture.componentInstance.actionMessageKey()).toBe('admin.disputes.resolveSuccess');
    expect(adminDisputeServiceStub.getDisputedBookings).toHaveBeenCalledTimes(2);
  });

  it('resolve() with REFUND calls the service with REFUND', () => {
    adminDisputeServiceStub.resolve.mockReturnValue(of(booking));
    const fixture = TestBed.createComponent(DisputeQueueComponent);
    fixture.detectChanges();

    fixture.componentInstance.resolve('booking-1', 'REFUND');

    expect(adminDisputeServiceStub.resolve).toHaveBeenCalledWith('booking-1', 'REFUND');
  });

  it('resolve() sets an error message when the request fails', () => {
    adminDisputeServiceStub.resolve.mockReturnValue(throwError(() => new Error('conflict')));
    const fixture = TestBed.createComponent(DisputeQueueComponent);
    fixture.detectChanges();

    fixture.componentInstance.resolve('booking-1', 'RELEASE');

    expect(fixture.componentInstance.actionMessageKey()).toBe('admin.disputes.actionError');
  });
});
