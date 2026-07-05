import { TestBed } from '@angular/core/testing';
import { provideRouter, Router } from '@angular/router';
import { provideHttpClient } from '@angular/common/http';
import { provideHttpClientTesting } from '@angular/common/http/testing';
import { provideNoopAnimations } from '@angular/platform-browser/animations';
import { provideTranslateService } from '@ngx-translate/core';
import { vi } from 'vitest';
import { of, throwError } from 'rxjs';
import { RegisterComponent } from './register.component';
import { AuthService } from '../../../core/auth/auth.service';

describe('RegisterComponent', () => {
  let authServiceStub: { register: ReturnType<typeof vi.fn> };
  let router: Router;

  beforeEach(async () => {
    authServiceStub = { register: vi.fn() };

    await TestBed.configureTestingModule({
      imports: [RegisterComponent],
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

  it('does not submit when the form is invalid (short password)', () => {
    const fixture = TestBed.createComponent(RegisterComponent);
    fixture.componentInstance.form.patchValue({ password: 'short' });
    fixture.componentInstance.submit();
    expect(authServiceStub.register).not.toHaveBeenCalled();
  });

  it('shows success and navigates to /login after successful registration', () => {
    vi.useFakeTimers();
    authServiceStub.register.mockReturnValue(of(undefined));
    const fixture = TestBed.createComponent(RegisterComponent);
    fixture.componentInstance.form.setValue({
      fullName: 'Yasmine',
      email: 'yasmine@example.com',
      password: 'supersecret1',
      role: 'STUDENT'
    });

    fixture.componentInstance.submit();
    expect(fixture.componentInstance.successKey()).toBe('auth.register.success');

    vi.advanceTimersByTime(1200);
    expect(router.navigateByUrl).toHaveBeenCalledWith('/login');
    vi.useRealTimers();
  });

  it('shows an error message when registration fails', () => {
    authServiceStub.register.mockReturnValue(throwError(() => new Error('conflict')));
    const fixture = TestBed.createComponent(RegisterComponent);
    fixture.componentInstance.form.setValue({
      fullName: 'Karim',
      email: 'karim@example.com',
      password: 'supersecret1',
      role: 'TUTOR'
    });

    fixture.componentInstance.submit();

    expect(fixture.componentInstance.errorKey()).toBe('auth.login.error');
  });
});
