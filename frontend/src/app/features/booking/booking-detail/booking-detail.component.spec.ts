import { TestBed } from '@angular/core/testing';
import { ActivatedRoute, convertToParamMap } from '@angular/router';
import { provideHttpClient } from '@angular/common/http';
import { provideHttpClientTesting } from '@angular/common/http/testing';
import { provideTranslateService } from '@ngx-translate/core';
import { vi, beforeEach } from 'vitest';
import { of, throwError } from 'rxjs';
import { BookingDetailComponent } from './booking-detail.component';
import { BookingService } from '../../../core/booking/booking.service';
import { AuthService } from '../../../core/auth/auth.service';
import { Booking } from '../../../core/booking/booking.models';
import { ReviewService } from '../../../core/review/review.service';
import { Review } from '../../../core/review/review.models';
import { MessageService } from '../../../core/messaging/message.service';
import { Message } from '../../../core/messaging/message.models';

describe('BookingDetailComponent', () => {
  let bookingServiceStub: {
    getBooking: ReturnType<typeof vi.fn>;
    completeBooking: ReturnType<typeof vi.fn>;
    disputeBooking: ReturnType<typeof vi.fn>;
  };
  let authServiceStub: { userId: ReturnType<typeof vi.fn> };
  let reviewServiceStub: {
    getReviews: ReturnType<typeof vi.fn>;
    submitReview: ReturnType<typeof vi.fn>;
  };
  let messageServiceStub: {
    getMessages: ReturnType<typeof vi.fn>;
    sendMessage: ReturnType<typeof vi.fn>;
  };

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

  const completedBooking: Booking = { ...escrowHeldBooking, status: 'COMPLETED', studentConfirmedAt: 'x', tutorConfirmedAt: 'y' };

  beforeEach(() => {
    reviewServiceStub = { getReviews: vi.fn().mockReturnValue(of([])), submitReview: vi.fn() };
    messageServiceStub = { getMessages: vi.fn().mockReturnValue(of([])), sendMessage: vi.fn() };
  });

  async function configure() {
    await TestBed.configureTestingModule({
      imports: [BookingDetailComponent],
      providers: [
        provideHttpClient(),
        provideHttpClientTesting(),
        provideTranslateService({ fallbackLang: 'fr' }),
        { provide: BookingService, useValue: bookingServiceStub },
        { provide: AuthService, useValue: authServiceStub },
        { provide: ReviewService, useValue: reviewServiceStub },
        { provide: MessageService, useValue: messageServiceStub },
        { provide: ActivatedRoute, useValue: { snapshot: { paramMap: convertToParamMap({ id: 'booking-1' }) } } }
      ]
    }).compileComponents();
  }

  it('loads the booking and shows the mark-complete button for the student when unconfirmed', async () => {
    bookingServiceStub = { getBooking: vi.fn().mockReturnValue(of(escrowHeldBooking)), completeBooking: vi.fn(), disputeBooking: vi.fn() };
    authServiceStub = { userId: vi.fn().mockReturnValue('student-1') };
    await configure();
    const fixture = TestBed.createComponent(BookingDetailComponent);
    fixture.detectChanges();

    expect(fixture.componentInstance.booking()).toEqual(escrowHeldBooking);
    expect(fixture.componentInstance.canConfirm(escrowHeldBooking)).toBe(true);
  });

  it('hides the mark-complete button once the current party already confirmed', async () => {
    const alreadyConfirmed = { ...escrowHeldBooking, studentConfirmedAt: '2026-07-15T01:00:00Z' };
    bookingServiceStub = { getBooking: vi.fn().mockReturnValue(of(alreadyConfirmed)), completeBooking: vi.fn(), disputeBooking: vi.fn() };
    authServiceStub = { userId: vi.fn().mockReturnValue('student-1') };
    await configure();
    const fixture = TestBed.createComponent(BookingDetailComponent);
    fixture.detectChanges();

    expect(fixture.componentInstance.canConfirm(alreadyConfirmed)).toBe(false);
  });

  it('does not show the mark-complete button for a non-party', async () => {
    bookingServiceStub = { getBooking: vi.fn().mockReturnValue(of(escrowHeldBooking)), completeBooking: vi.fn(), disputeBooking: vi.fn() };
    authServiceStub = { userId: vi.fn().mockReturnValue('someone-else') };
    await configure();
    const fixture = TestBed.createComponent(BookingDetailComponent);
    fixture.detectChanges();

    expect(fixture.componentInstance.canConfirm(escrowHeldBooking)).toBe(false);
    expect(fixture.componentInstance.canDispute(escrowHeldBooking)).toBe(false);
  });

  it('confirm() calls completeBooking and updates the booking signal', async () => {
    const completed = { ...escrowHeldBooking, status: 'COMPLETED' as const, studentConfirmedAt: 'x', tutorConfirmedAt: 'y' };
    bookingServiceStub = {
      getBooking: vi.fn().mockReturnValue(of(escrowHeldBooking)),
      completeBooking: vi.fn().mockReturnValue(of(completed)),
      disputeBooking: vi.fn()
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

  it('a party can dispute an escrow-held booking', async () => {
    bookingServiceStub = { getBooking: vi.fn().mockReturnValue(of(escrowHeldBooking)), completeBooking: vi.fn(), disputeBooking: vi.fn() };
    authServiceStub = { userId: vi.fn().mockReturnValue('student-1') };
    await configure();
    const fixture = TestBed.createComponent(BookingDetailComponent);
    fixture.detectChanges();

    expect(fixture.componentInstance.canDispute(escrowHeldBooking)).toBe(true);
  });

  it('dispute() calls disputeBooking and updates the booking signal', async () => {
    const disputed = { ...escrowHeldBooking, status: 'DISPUTED' as const };
    bookingServiceStub = {
      getBooking: vi.fn().mockReturnValue(of(escrowHeldBooking)),
      completeBooking: vi.fn(),
      disputeBooking: vi.fn().mockReturnValue(of(disputed))
    };
    authServiceStub = { userId: vi.fn().mockReturnValue('student-1') };
    await configure();
    const fixture = TestBed.createComponent(BookingDetailComponent);
    fixture.detectChanges();

    fixture.componentInstance.dispute();

    expect(bookingServiceStub.disputeBooking).toHaveBeenCalledWith('booking-1');
    expect(fixture.componentInstance.booking()).toEqual(disputed);
    expect(fixture.componentInstance.disputing()).toBe(false);
  });

  it('sets the error state when the booking fails to load', async () => {
    bookingServiceStub = { getBooking: vi.fn().mockReturnValue(throwError(() => new Error('403'))), completeBooking: vi.fn(), disputeBooking: vi.fn() };
    authServiceStub = { userId: vi.fn().mockReturnValue('someone') };
    await configure();
    const fixture = TestBed.createComponent(BookingDetailComponent);
    fixture.detectChanges();

    expect(fixture.componentInstance.error()).toBe(true);
  });

  it('loads reviews once a booking is completed', async () => {
    const reviews: Review[] = [
      { id: 'r1', bookingId: 'booking-1', reviewerId: 'tutor-1', rating: 4, comment: 'Good student', createdAt: 'x' }
    ];
    bookingServiceStub = { getBooking: vi.fn().mockReturnValue(of(completedBooking)), completeBooking: vi.fn(), disputeBooking: vi.fn() };
    authServiceStub = { userId: vi.fn().mockReturnValue('student-1') };
    reviewServiceStub = { getReviews: vi.fn().mockReturnValue(of(reviews)), submitReview: vi.fn() };
    await configure();
    const fixture = TestBed.createComponent(BookingDetailComponent);
    fixture.detectChanges();

    expect(reviewServiceStub.getReviews).toHaveBeenCalledWith('booking-1');
    expect(fixture.componentInstance.reviews()).toEqual(reviews);
    expect(fixture.componentInstance.hasReviewed()).toBe(false);
  });

  it('hasReviewed() is true once the current user has already submitted a review', async () => {
    const reviews: Review[] = [
      { id: 'r1', bookingId: 'booking-1', reviewerId: 'student-1', rating: 5, comment: null, createdAt: 'x' }
    ];
    bookingServiceStub = { getBooking: vi.fn().mockReturnValue(of(completedBooking)), completeBooking: vi.fn(), disputeBooking: vi.fn() };
    authServiceStub = { userId: vi.fn().mockReturnValue('student-1') };
    reviewServiceStub = { getReviews: vi.fn().mockReturnValue(of(reviews)), submitReview: vi.fn() };
    await configure();
    const fixture = TestBed.createComponent(BookingDetailComponent);
    fixture.detectChanges();

    expect(fixture.componentInstance.hasReviewed()).toBe(true);
  });

  it('submitReview() calls the review service and appends the result', async () => {
    const newReview: Review = { id: 'r2', bookingId: 'booking-1', reviewerId: 'student-1', rating: 5, comment: 'Great', createdAt: 'x' };
    bookingServiceStub = { getBooking: vi.fn().mockReturnValue(of(completedBooking)), completeBooking: vi.fn(), disputeBooking: vi.fn() };
    authServiceStub = { userId: vi.fn().mockReturnValue('student-1') };
    reviewServiceStub = { getReviews: vi.fn().mockReturnValue(of([])), submitReview: vi.fn().mockReturnValue(of(newReview)) };
    await configure();
    const fixture = TestBed.createComponent(BookingDetailComponent);
    fixture.detectChanges();

    fixture.componentInstance.submitReview();

    expect(reviewServiceStub.submitReview).toHaveBeenCalledWith('booking-1', { rating: 5, comment: null });
    expect(fixture.componentInstance.reviews()).toEqual([newReview]);
    expect(fixture.componentInstance.submittingReview()).toBe(false);
  });

  it('submitReview() sets the error state when the request fails', async () => {
    bookingServiceStub = { getBooking: vi.fn().mockReturnValue(of(completedBooking)), completeBooking: vi.fn(), disputeBooking: vi.fn() };
    authServiceStub = { userId: vi.fn().mockReturnValue('student-1') };
    reviewServiceStub = { getReviews: vi.fn().mockReturnValue(of([])), submitReview: vi.fn().mockReturnValue(throwError(() => new Error('409'))) };
    await configure();
    const fixture = TestBed.createComponent(BookingDetailComponent);
    fixture.detectChanges();

    fixture.componentInstance.submitReview();

    expect(fixture.componentInstance.reviewError()).toBe(true);
    expect(fixture.componentInstance.submittingReview()).toBe(false);
  });

  it('loads messages for the booking regardless of status', async () => {
    const messages: Message[] = [{ id: 'm1', senderId: 'tutor-1', body: 'Hi', createdAt: 'x' }];
    bookingServiceStub = { getBooking: vi.fn().mockReturnValue(of(escrowHeldBooking)), completeBooking: vi.fn(), disputeBooking: vi.fn() };
    authServiceStub = { userId: vi.fn().mockReturnValue('student-1') };
    messageServiceStub = { getMessages: vi.fn().mockReturnValue(of(messages)), sendMessage: vi.fn() };
    await configure();
    const fixture = TestBed.createComponent(BookingDetailComponent);
    fixture.detectChanges();

    expect(messageServiceStub.getMessages).toHaveBeenCalledWith('booking-1');
    expect(fixture.componentInstance.messages()).toEqual(messages);
  });

  it('sendMessage() calls the message service and appends the result', async () => {
    const newMessage: Message = { id: 'm2', senderId: 'student-1', body: 'See you then', createdAt: 'x' };
    bookingServiceStub = { getBooking: vi.fn().mockReturnValue(of(escrowHeldBooking)), completeBooking: vi.fn(), disputeBooking: vi.fn() };
    authServiceStub = { userId: vi.fn().mockReturnValue('student-1') };
    messageServiceStub = { getMessages: vi.fn().mockReturnValue(of([])), sendMessage: vi.fn().mockReturnValue(of(newMessage)) };
    await configure();
    const fixture = TestBed.createComponent(BookingDetailComponent);
    fixture.detectChanges();

    fixture.componentInstance.messageForm.controls.body.setValue('See you then');
    fixture.componentInstance.sendMessage();

    expect(messageServiceStub.sendMessage).toHaveBeenCalledWith('booking-1', { body: 'See you then' });
    expect(fixture.componentInstance.messages()).toEqual([newMessage]);
    expect(fixture.componentInstance.sendingMessage()).toBe(false);
  });

  it('sendMessage() does not call the service when the form is invalid', async () => {
    bookingServiceStub = { getBooking: vi.fn().mockReturnValue(of(escrowHeldBooking)), completeBooking: vi.fn(), disputeBooking: vi.fn() };
    authServiceStub = { userId: vi.fn().mockReturnValue('student-1') };
    messageServiceStub = { getMessages: vi.fn().mockReturnValue(of([])), sendMessage: vi.fn() };
    await configure();
    const fixture = TestBed.createComponent(BookingDetailComponent);
    fixture.detectChanges();

    fixture.componentInstance.messageForm.controls.body.setValue('');
    fixture.componentInstance.sendMessage();

    expect(messageServiceStub.sendMessage).not.toHaveBeenCalled();
  });
});
