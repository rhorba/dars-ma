import { TestBed } from '@angular/core/testing';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { provideHttpClient } from '@angular/common/http';
import { ProfileService } from './profile.service';
import { TutorProfile } from './profile.models';

describe('ProfileService', () => {
  let service: ProfileService;
  let httpMock: HttpTestingController;

  const sampleProfile: TutorProfile = {
    userId: 'user-1',
    bio: 'Bio',
    subjects: ['Math'],
    hourlyRateMad: 100,
    verificationStatus: 'PENDING',
    avgRating: null
  };

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [provideHttpClient(), provideHttpClientTesting()]
    });
    service = TestBed.inject(ProfileService);
    httpMock = TestBed.inject(HttpTestingController);
  });

  afterEach(() => httpMock.verify());

  it('getMyTutorProfile() GETs /api/v1/profile/tutor/me', () => {
    service.getMyTutorProfile().subscribe((profile) => expect(profile).toEqual(sampleProfile));
    const req = httpMock.expectOne('/api/v1/profile/tutor/me');
    expect(req.request.method).toBe('GET');
    req.flush(sampleProfile);
  });

  it('upsertMyTutorProfile() PUTs the request body', () => {
    service.upsertMyTutorProfile({ bio: 'Bio', subjects: ['Math'], hourlyRateMad: 100 }).subscribe();
    const req = httpMock.expectOne('/api/v1/profile/tutor/me');
    expect(req.request.method).toBe('PUT');
    expect(req.request.body).toEqual({ bio: 'Bio', subjects: ['Math'], hourlyRateMad: 100 });
    req.flush(sampleProfile);
  });

  it('getPublicTutorProfile() GETs the public endpoint by userId', () => {
    service.getPublicTutorProfile('user-1').subscribe();
    const req = httpMock.expectOne('/api/v1/profile/tutor/user-1');
    expect(req.request.method).toBe('GET');
    req.flush(sampleProfile);
  });

  it('uploadVerificationDocument() POSTs multipart form data', () => {
    const file = new File(['content'], 'diploma.pdf', { type: 'application/pdf' });
    service.uploadVerificationDocument('DIPLOMA', file).subscribe();
    const req = httpMock.expectOne('/api/v1/profile/tutor/me/verification-documents');
    expect(req.request.method).toBe('POST');
    expect(req.request.body instanceof FormData).toBe(true);
    req.flush({ id: 'doc-1', docType: 'DIPLOMA', originalFilename: 'diploma.pdf', reviewedAt: null, createdAt: '2026-01-01T00:00:00Z' });
  });

  it('browseTutors() GETs /api/v1/profile/tutor without params when no subject given', () => {
    service.browseTutors().subscribe((tutors) => expect(tutors).toEqual([sampleProfile]));
    const req = httpMock.expectOne('/api/v1/profile/tutor');
    expect(req.request.method).toBe('GET');
    req.flush([sampleProfile]);
  });

  it('browseTutors() GETs /api/v1/profile/tutor with a subject query param', () => {
    service.browseTutors('Math').subscribe();
    const req = httpMock.expectOne('/api/v1/profile/tutor?subject=Math');
    expect(req.request.method).toBe('GET');
    req.flush([sampleProfile]);
  });
});
