import { TestBed } from '@angular/core/testing';
import { Router, UrlTree } from '@angular/router';
import { provideRouter } from '@angular/router';
import { authGuard } from './auth.guard';
import { AuthService } from './auth.service';

describe('authGuard', () => {
  let authServiceStub: { isAuthenticated: () => boolean };

  function runGuard() {
    return TestBed.runInInjectionContext(() => authGuard({} as any, {} as any));
  }

  beforeEach(() => {
    authServiceStub = { isAuthenticated: () => false };
    TestBed.configureTestingModule({
      providers: [provideRouter([]), { provide: AuthService, useValue: authServiceStub }]
    });
  });

  it('allows navigation when authenticated', () => {
    authServiceStub.isAuthenticated = () => true;
    expect(runGuard()).toBe(true);
  });

  it('redirects to /login when not authenticated', () => {
    authServiceStub.isAuthenticated = () => false;
    const result = runGuard();
    expect(result).toBeInstanceOf(UrlTree);

    const router = TestBed.inject(Router);
    expect(router.serializeUrl(result as UrlTree)).toBe('/login');
  });
});
