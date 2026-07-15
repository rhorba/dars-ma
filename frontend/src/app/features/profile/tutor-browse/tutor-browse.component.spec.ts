import { TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';
import { provideHttpClient } from '@angular/common/http';
import { provideHttpClientTesting } from '@angular/common/http/testing';
import { provideTranslateService } from '@ngx-translate/core';
import { vi } from 'vitest';
import { of, throwError } from 'rxjs';
import { TutorBrowseComponent } from './tutor-browse.component';
import { ProfileService } from '../../../core/profile/profile.service';
import { TutorProfile } from '../../../core/profile/profile.models';

describe('TutorBrowseComponent', () => {
  let profileServiceStub: { browseTutors: ReturnType<typeof vi.fn> };

  const tutor: TutorProfile = {
    userId: 'tutor-1',
    bio: 'Bio',
    subjects: ['Math'],
    hourlyRateMad: 100,
    verificationStatus: 'VERIFIED',
    avgRating: 4.5
  };

  function configure() {
    return TestBed.configureTestingModule({
      imports: [TutorBrowseComponent],
      providers: [
        provideRouter([]),
        provideHttpClient(),
        provideHttpClientTesting(),
        provideTranslateService({ fallbackLang: 'fr' }),
        { provide: ProfileService, useValue: profileServiceStub }
      ]
    }).compileComponents();
  }

  it('loads all verified tutors on init', async () => {
    profileServiceStub = { browseTutors: vi.fn().mockReturnValue(of([tutor])) };
    await configure();
    const fixture = TestBed.createComponent(TutorBrowseComponent);
    fixture.detectChanges();

    expect(profileServiceStub.browseTutors).toHaveBeenCalledWith(undefined);
    expect(fixture.componentInstance.tutors()).toEqual([tutor]);
    expect(fixture.componentInstance.loading()).toBe(false);
  });

  it('shows the error state when the request fails', async () => {
    profileServiceStub = { browseTutors: vi.fn().mockReturnValue(throwError(() => new Error('fail'))) };
    await configure();
    const fixture = TestBed.createComponent(TutorBrowseComponent);
    fixture.detectChanges();

    expect(fixture.componentInstance.error()).toBe(true);
  });

  it('reloads with the subject filter', async () => {
    profileServiceStub = { browseTutors: vi.fn().mockReturnValue(of([])) };
    await configure();
    const fixture = TestBed.createComponent(TutorBrowseComponent);
    fixture.detectChanges();
    fixture.componentInstance.subjectFilter = 'Physics';

    fixture.componentInstance.load();

    expect(profileServiceStub.browseTutors).toHaveBeenLastCalledWith('Physics');
    expect(fixture.componentInstance.tutors()).toEqual([]);
  });
});
