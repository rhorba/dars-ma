import { TestBed } from '@angular/core/testing';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { provideHttpClient } from '@angular/common/http';
import { ReviewService } from './review.service';
import { Review } from './review.models';

describe('ReviewService', () => {
  let service: ReviewService;
  let httpMock: HttpTestingController;

  const sampleReview: Review = {
    id: 'review-1',
    bookingId: 'booking-1',
    reviewerId: 'student-1',
    rating: 5,
    comment: 'Great session',
    createdAt: '2026-07-16T00:00:00Z'
  };

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [provideHttpClient(), provideHttpClientTesting()]
    });
    service = TestBed.inject(ReviewService);
    httpMock = TestBed.inject(HttpTestingController);
  });

  afterEach(() => httpMock.verify());

  it('submitReview() POSTs the request body to /api/v1/bookings/:id/reviews', () => {
    service
      .submitReview('booking-1', { rating: 5, comment: 'Great session' })
      .subscribe((review) => expect(review).toEqual(sampleReview));
    const req = httpMock.expectOne('/api/v1/bookings/booking-1/reviews');
    expect(req.request.method).toBe('POST');
    expect(req.request.body).toEqual({ rating: 5, comment: 'Great session' });
    req.flush(sampleReview);
  });

  it('getReviews() GETs /api/v1/bookings/:id/reviews', () => {
    service.getReviews('booking-1').subscribe((reviews) => expect(reviews).toEqual([sampleReview]));
    const req = httpMock.expectOne('/api/v1/bookings/booking-1/reviews');
    expect(req.request.method).toBe('GET');
    req.flush([sampleReview]);
  });
});
