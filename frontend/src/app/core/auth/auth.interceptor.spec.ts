import { TestBed } from '@angular/core/testing';
import { HttpClient, provideHttpClient, withInterceptors } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { authInterceptor } from './auth.interceptor';
import { AuthService } from './auth.service';

describe('authInterceptor', () => {
  let http: HttpClient;
  let httpMock: HttpTestingController;
  let authService: AuthService;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [
        provideHttpClient(withInterceptors([authInterceptor])),
        provideHttpClientTesting()
      ]
    });
    http = TestBed.inject(HttpClient);
    httpMock = TestBed.inject(HttpTestingController);
    authService = TestBed.inject(AuthService);
  });

  afterEach(() => httpMock.verify());

  it('attaches the Authorization header when a token is present', () => {
    (authService as any).accessToken.set('token-123');

    http.get('/api/v1/gigs').subscribe();

    const req = httpMock.expectOne('/api/v1/gigs');
    expect(req.request.headers.get('Authorization')).toBe('Bearer token-123');
    req.flush({});
  });

  it('does not attach the Authorization header to auth endpoints', () => {
    (authService as any).accessToken.set('token-123');

    http.post('/api/v1/auth/login', {}).subscribe();

    const req = httpMock.expectOne('/api/v1/auth/login');
    expect(req.request.headers.has('Authorization')).toBe(false);
    req.flush({});
  });

  it('on a 401, refreshes the token once and retries the original request', () => {
    http.get('/api/v1/gigs').subscribe();

    const firstAttempt = httpMock.expectOne('/api/v1/gigs');
    firstAttempt.flush({ message: 'expired' }, { status: 401, statusText: 'Unauthorized' });

    const refreshReq = httpMock.expectOne('/api/v1/auth/refresh');
    refreshReq.flush({ accessToken: 'new-token', accessTokenExpiresInSeconds: 900 });

    const retryReq = httpMock.expectOne('/api/v1/gigs');
    expect(retryReq.request.headers.get('Authorization')).toBe('Bearer new-token');
    retryReq.flush({});
  });

  it('logs out when the refresh itself fails', () => {
    (authService as any).accessToken.set('stale-token');

    http.get('/api/v1/gigs').subscribe({ error: () => {} });

    httpMock.expectOne('/api/v1/gigs').flush({}, { status: 401, statusText: 'Unauthorized' });
    httpMock.expectOne('/api/v1/auth/refresh').flush({}, { status: 401, statusText: 'Unauthorized' });

    expect(authService.isAuthenticated()).toBe(false);
  });
});
