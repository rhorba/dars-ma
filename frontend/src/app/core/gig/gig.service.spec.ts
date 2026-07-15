import { TestBed } from '@angular/core/testing';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { provideHttpClient } from '@angular/common/http';
import { GigService } from './gig.service';
import { GigRequest, MatchSuggestion } from './gig.models';

describe('GigService', () => {
  let service: GigService;
  let httpMock: HttpTestingController;

  const sampleGig: GigRequest = {
    id: 'gig-1',
    studentUserId: 'user-1',
    subject: 'Math',
    level: 'High School',
    description: 'Need help with calculus',
    budgetMinMad: 100,
    budgetMaxMad: 200,
    status: 'OPEN',
    createdAt: '2026-07-06T00:00:00Z'
  };

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [provideHttpClient(), provideHttpClientTesting()]
    });
    service = TestBed.inject(GigService);
    httpMock = TestBed.inject(HttpTestingController);
  });

  afterEach(() => httpMock.verify());

  it('createGig() POSTs the request body to /api/v1/gigs', () => {
    service
      .createGig({ subject: 'Math', level: 'High School', description: 'Need help with calculus', budgetMinMad: 100, budgetMaxMad: 200 })
      .subscribe((gig) => expect(gig).toEqual(sampleGig));
    const req = httpMock.expectOne('/api/v1/gigs');
    expect(req.request.method).toBe('POST');
    expect(req.request.body).toEqual({
      subject: 'Math',
      level: 'High School',
      description: 'Need help with calculus',
      budgetMinMad: 100,
      budgetMaxMad: 200
    });
    req.flush(sampleGig);
  });

  it('getGig() GETs /api/v1/gigs/:id', () => {
    service.getGig('gig-1').subscribe((gig) => expect(gig).toEqual(sampleGig));
    const req = httpMock.expectOne('/api/v1/gigs/gig-1');
    expect(req.request.method).toBe('GET');
    req.flush(sampleGig);
  });

  it('getMatches() GETs /api/v1/gigs/:id/matches', () => {
    const matches: MatchSuggestion[] = [{ tutorUserId: 'tutor-1', similarityScore: 0.9 }];
    service.getMatches('gig-1').subscribe((result) => expect(result).toEqual(matches));
    const req = httpMock.expectOne('/api/v1/gigs/gig-1/matches');
    expect(req.request.method).toBe('GET');
    req.flush(matches);
  });
});
