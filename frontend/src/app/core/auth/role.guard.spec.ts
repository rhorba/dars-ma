import { TestBed } from '@angular/core/testing';
import { Router, UrlTree } from '@angular/router';
import { provideRouter } from '@angular/router';
import { roleGuard } from './role.guard';
import { AuthService } from './auth.service';
import { Role } from './auth.models';

describe('roleGuard', () => {
  let authServiceStub: { isAuthenticated: () => boolean; role: () => Role | null };

  function runGuard(allowed: Role[]) {
    return TestBed.runInInjectionContext(() => roleGuard(allowed)({} as any, {} as any));
  }

  beforeEach(() => {
    authServiceStub = { isAuthenticated: () => false, role: () => null };
    TestBed.configureTestingModule({
      providers: [provideRouter([]), { provide: AuthService, useValue: authServiceStub }]
    });
  });

  it('allows navigation when authenticated with an allowed role', () => {
    authServiceStub.isAuthenticated = () => true;
    authServiceStub.role = () => 'TUTOR';
    expect(runGuard(['TUTOR'])).toBe(true);
  });

  it('redirects to /login when authenticated with a disallowed role', () => {
    authServiceStub.isAuthenticated = () => true;
    authServiceStub.role = () => 'STUDENT';
    const result = runGuard(['TUTOR']);
    expect(result).toBeInstanceOf(UrlTree);
  });

  it('redirects to /login when not authenticated', () => {
    authServiceStub.isAuthenticated = () => false;
    authServiceStub.role = () => null;
    const result = runGuard(['ADMIN']);
    const router = TestBed.inject(Router);
    expect(router.serializeUrl(result as UrlTree)).toBe('/login');
  });
});
