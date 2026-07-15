import { TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';
import { provideHttpClient } from '@angular/common/http';
import { provideHttpClientTesting } from '@angular/common/http/testing';
import { provideNoopAnimations } from '@angular/platform-browser/animations';
import { provideTranslateService } from '@ngx-translate/core';
import { vi } from 'vitest';
import { of, throwError } from 'rxjs';
import { GigCreateFormComponent } from './gig-create-form.component';
import { GigService } from '../../../core/gig/gig.service';
import { GigRequest } from '../../../core/gig/gig.models';

describe('GigCreateFormComponent', () => {
  let gigServiceStub: { createGig: ReturnType<typeof vi.fn> };

  const createdGig: GigRequest = {
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

  beforeEach(async () => {
    gigServiceStub = { createGig: vi.fn() };

    await TestBed.configureTestingModule({
      imports: [GigCreateFormComponent],
      providers: [
        provideRouter([]),
        provideHttpClient(),
        provideHttpClientTesting(),
        provideNoopAnimations(),
        provideTranslateService({ fallbackLang: 'fr' }),
        { provide: GigService, useValue: gigServiceStub }
      ]
    }).compileComponents();
  });

  it('does not submit when the form is invalid', () => {
    const fixture = TestBed.createComponent(GigCreateFormComponent);
    fixture.detectChanges();

    fixture.componentInstance.submit();

    expect(gigServiceStub.createGig).not.toHaveBeenCalled();
    expect(fixture.componentInstance.form.controls.subject.touched).toBe(true);
  });

  it('rejects budgetMinMad greater than budgetMaxMad client-side without calling the API', () => {
    const fixture = TestBed.createComponent(GigCreateFormComponent);
    fixture.detectChanges();
    fixture.componentInstance.form.setValue({
      subject: 'Math',
      level: 'High School',
      description: 'Need help with calculus',
      budgetMinMad: 300,
      budgetMaxMad: 200
    });

    fixture.componentInstance.submit();

    expect(gigServiceStub.createGig).not.toHaveBeenCalled();
    expect(fixture.componentInstance.errorMessageKey()).toBe('gig.create.budgetRangeError');
  });

  it('submits valid input and shows a success message', () => {
    gigServiceStub.createGig.mockReturnValue(of(createdGig));
    const fixture = TestBed.createComponent(GigCreateFormComponent);
    fixture.detectChanges();
    fixture.componentInstance.form.setValue({
      subject: 'Math',
      level: 'High School',
      description: 'Need help with calculus',
      budgetMinMad: 100,
      budgetMaxMad: 200
    });

    fixture.componentInstance.submit();

    expect(gigServiceStub.createGig).toHaveBeenCalledWith({
      subject: 'Math',
      level: 'High School',
      description: 'Need help with calculus',
      budgetMinMad: 100,
      budgetMaxMad: 200
    });
    expect(fixture.componentInstance.createdGig()).toEqual(createdGig);
  });

  it('shows an error message when submission fails', () => {
    gigServiceStub.createGig.mockReturnValue(throwError(() => new Error('bad request')));
    const fixture = TestBed.createComponent(GigCreateFormComponent);
    fixture.detectChanges();
    fixture.componentInstance.form.setValue({
      subject: 'Math',
      level: 'High School',
      description: 'Need help with calculus',
      budgetMinMad: null,
      budgetMaxMad: null
    });

    fixture.componentInstance.submit();

    expect(fixture.componentInstance.errorMessageKey()).toBe('gig.create.submitError');
  });
});
