import { TestBed } from '@angular/core/testing';
import { provideRouter, Router } from '@angular/router';
import { provideHttpClient } from '@angular/common/http';
import { provideHttpClientTesting } from '@angular/common/http/testing';
import { provideNoopAnimations } from '@angular/platform-browser/animations';
import { provideTranslateService } from '@ngx-translate/core';
import { vi } from 'vitest';
import { of, throwError } from 'rxjs';
import { LoginComponent } from './login.component';
import { AuthService } from '../../../core/auth/auth.service';

describe('LoginComponent', () => {
  let authServiceStub: { login: ReturnType<typeof vi.fn> };
  let router: Router;

  beforeEach(async () => {
    authServiceStub = { login: vi.fn() };

    await TestBed.configureTestingModule({
      imports: [LoginComponent],
      providers: [
        provideRouter([]),
        provideHttpClient(),
        provideHttpClientTesting(),
        provideNoopAnimations(),
        provideTranslateService({ fallbackLang: 'fr' }),
        { provide: AuthService, useValue: authServiceStub }
      ]
    }).compileComponents();

    router = TestBed.inject(Router);
    vi.spyOn(router, 'navigateByUrl');
  });

  it('does not submit when the form is invalid', () => {
    const fixture = TestBed.createComponent(LoginComponent);
    fixture.componentInstance.submit();
    expect(authServiceStub.login).not.toHaveBeenCalled();
  });

  it('navigates home on successful login', () => {
    authServiceStub.login.mockReturnValue(of({ accessToken: 't', accessTokenExpiresInSeconds: 900 }));
    const fixture = TestBed.createComponent(LoginComponent);
    fixture.componentInstance.form.setValue({ email: 'karim@example.com', password: 'supersecret1' });

    fixture.componentInstance.submit();

    expect(authServiceStub.login).toHaveBeenCalledWith({ email: 'karim@example.com', password: 'supersecret1' });
    expect(router.navigateByUrl).toHaveBeenCalledWith('/');
  });

  it('shows an error message when login fails', () => {
    authServiceStub.login.mockReturnValue(throwError(() => new Error('unauthorized')));
    const fixture = TestBed.createComponent(LoginComponent);
    fixture.componentInstance.form.setValue({ email: 'karim@example.com', password: 'wrong' });

    fixture.componentInstance.submit();

    expect(fixture.componentInstance.errorKey()).toBe('auth.login.error');
    expect(router.navigateByUrl).not.toHaveBeenCalled();
  });
});
