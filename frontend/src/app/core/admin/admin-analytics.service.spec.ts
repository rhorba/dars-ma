import { TestBed } from '@angular/core/testing';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { provideHttpClient } from '@angular/common/http';
import { AdminAnalyticsService } from './admin-analytics.service';
import { AdminAnalytics } from './admin.models';

describe('AdminAnalyticsService', () => {
  let service: AdminAnalyticsService;
  let httpMock: HttpTestingController;

  const analytics: AdminAnalytics = {
    studentSignups: 10,
    tutorSignups: 4,
    totalBookings: 20,
    completedBookings: 12,
    gmvMad: 2400,
    matchRatePercent: 80
  };

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [provideHttpClient(), provideHttpClientTesting()]
    });
    service = TestBed.inject(AdminAnalyticsService);
    httpMock = TestBed.inject(HttpTestingController);
  });

  afterEach(() => httpMock.verify());

  it('getAnalytics() GETs /api/v1/admin/analytics', () => {
    service.getAnalytics().subscribe((result) => expect(result).toEqual(analytics));
    const req = httpMock.expectOne('/api/v1/admin/analytics');
    expect(req.request.method).toBe('GET');
    req.flush(analytics);
  });
});
