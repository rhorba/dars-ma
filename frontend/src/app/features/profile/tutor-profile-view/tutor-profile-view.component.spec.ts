import { TestBed } from '@angular/core/testing';
import { ActivatedRoute, convertToParamMap } from '@angular/router';
import { provideHttpClient } from '@angular/common/http';
import { provideHttpClientTesting } from '@angular/common/http/testing';
import { provideTranslateService } from '@ngx-translate/core';
import { vi } from 'vitest';
import { of, throwError } from 'rxjs';
import { TutorProfileViewComponent } from './tutor-profile-view.component';
import { ProfileService } from '../../../core/profile/profile.service';
import { TutorProfile } from '../../../core/profile/profile.models';

describe('TutorProfileViewComponent', () => {
  let profileServiceStub: { getPublicTutorProfile: ReturnType<typeof vi.fn> };

  const profile: TutorProfile = {
    userId: 'user-1',
    bio: 'Bio',
    subjects: ['Math'],
    hourlyRateMad: 100,
    verificationStatus: 'VERIFIED',
    avgRating: 4.5
  };

  function configure(userId: string | null) {
    profileServiceStub = { getPublicTutorProfile: vi.fn().mockReturnValue(of(profile)) };
    return TestBed.configureTestingModule({
      imports: [TutorProfileViewComponent],
      providers: [
        provideHttpClient(),
        provideHttpClientTesting(),
        provideTranslateService({ fallbackLang: 'fr' }),
        { provide: ProfileService, useValue: profileServiceStub },
        {
          provide: ActivatedRoute,
          useValue: { snapshot: { paramMap: convertToParamMap(userId ? { userId } : {}) } }
        }
      ]
    }).compileComponents();
  }

  it('loads the public profile for the route userId', async () => {
    await configure('user-1');
    const fixture = TestBed.createComponent(TutorProfileViewComponent);
    fixture.detectChanges();

    expect(profileServiceStub.getPublicTutorProfile).toHaveBeenCalledWith('user-1');
    expect(fixture.componentInstance.profile()).toEqual(profile);
  });

  it('sets notFound when the profile request fails', async () => {
    await configure('user-1');
    profileServiceStub.getPublicTutorProfile.mockReturnValue(throwError(() => new Error('404')));
    const fixture = TestBed.createComponent(TutorProfileViewComponent);
    fixture.detectChanges();

    expect(fixture.componentInstance.notFound()).toBe(true);
  });

  it('sets notFound immediately when no userId param is present', async () => {
    await configure(null);
    const fixture = TestBed.createComponent(TutorProfileViewComponent);
    fixture.detectChanges();

    expect(fixture.componentInstance.notFound()).toBe(true);
    expect(profileServiceStub.getPublicTutorProfile).not.toHaveBeenCalled();
  });
});
