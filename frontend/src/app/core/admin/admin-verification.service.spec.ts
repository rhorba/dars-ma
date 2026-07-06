import { TestBed } from '@angular/core/testing';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { provideHttpClient } from '@angular/common/http';
import { AdminVerificationService } from './admin-verification.service';

describe('AdminVerificationService', () => {
  let service: AdminVerificationService;
  let httpMock: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [provideHttpClient(), provideHttpClientTesting()]
    });
    service = TestBed.inject(AdminVerificationService);
    httpMock = TestBed.inject(HttpTestingController);
  });

  afterEach(() => httpMock.verify());

  it('getQueue() GETs the queue endpoint', () => {
    service.getQueue().subscribe();
    const req = httpMock.expectOne('/api/v1/admin/verification/queue');
    expect(req.request.method).toBe('GET');
    req.flush([]);
  });

  it('approve() POSTs to the approve endpoint', () => {
    service.approve('doc-1').subscribe();
    const req = httpMock.expectOne('/api/v1/admin/verification/documents/doc-1/approve');
    expect(req.request.method).toBe('POST');
    req.flush(null);
  });

  it('reject() POSTs the reason to the reject endpoint', () => {
    service.reject('doc-1', 'blurry').subscribe();
    const req = httpMock.expectOne('/api/v1/admin/verification/documents/doc-1/reject');
    expect(req.request.method).toBe('POST');
    expect(req.request.body).toEqual({ reason: 'blurry' });
    req.flush(null);
  });
});
