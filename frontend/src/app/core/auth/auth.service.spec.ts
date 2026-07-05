import { TestBed } from '@angular/core/testing';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { provideHttpClient } from '@angular/common/http';
import { AuthService } from './auth.service';

// A JWT with payload {"sub":"user-1","role":"STUDENT","exp":9999999999} — signature is irrelevant client-side.
// gitleaks:allow — fake test fixture, not a real credential.
const FAKE_ACCESS_TOKEN =
  'eyJhbGciOiJIUzI1NiJ9.eyJzdWIiOiJ1c2VyLTEiLCJyb2xlIjoiU1RVREVOVCIsImV4cCI6OTk5OTk5OTk5OX0.signature';

describe('AuthService', () => {
  let service: AuthService;
  let httpMock: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [provideHttpClient(), provideHttpClientTesting()]
    });
    service = TestBed.inject(AuthService);
    httpMock = TestBed.inject(HttpTestingController);
  });

  afterEach(() => httpMock.verify());

  it('is not authenticated before any login', () => {
    expect(service.isAuthenticated()).toBe(false);
    expect(service.getAccessToken()).toBeNull();
  });

  it('login() stores the access token and decodes the role', () => {
    service.login({ email: 'karim@example.com', password: 'supersecret1' }).subscribe();

    const req = httpMock.expectOne('/api/v1/auth/login');
    expect(req.request.method).toBe('POST');
    req.flush({ accessToken: FAKE_ACCESS_TOKEN, accessTokenExpiresInSeconds: 900 });

    expect(service.isAuthenticated()).toBe(true);
    expect(service.role()).toBe('STUDENT');
    expect(service.getAccessToken()).toBe(FAKE_ACCESS_TOKEN);
  });

  it('register() posts to the register endpoint', () => {
    service
      .register({ email: 'yasmine@example.com', password: 'supersecret1', role: 'STUDENT', fullName: 'Yasmine' })
      .subscribe();

    const req = httpMock.expectOne('/api/v1/auth/register');
    expect(req.request.method).toBe('POST');
    req.flush(null);
  });

  it('logout() clears the access token', () => {
    service.login({ email: 'karim@example.com', password: 'supersecret1' }).subscribe();
    httpMock.expectOne('/api/v1/auth/login').flush({ accessToken: FAKE_ACCESS_TOKEN, accessTokenExpiresInSeconds: 900 });
    expect(service.isAuthenticated()).toBe(true);

    service.logout();

    expect(service.isAuthenticated()).toBe(false);
    expect(service.getAccessToken()).toBeNull();
  });
});
