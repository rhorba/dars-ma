import { TestBed } from '@angular/core/testing';
import { ActivatedRoute, convertToParamMap, provideRouter } from '@angular/router';
import { provideHttpClient } from '@angular/common/http';
import { provideHttpClientTesting } from '@angular/common/http/testing';
import { provideTranslateService } from '@ngx-translate/core';
import { vi } from 'vitest';
import { of, throwError } from 'rxjs';
import { GigDetailComponent } from './gig-detail.component';
import { GigService } from '../../../core/gig/gig.service';
import { ProfileService } from '../../../core/profile/profile.service';
import { GigRequest, MatchSuggestion } from '../../../core/gig/gig.models';
import { TutorProfile } from '../../../core/profile/profile.models';

describe('GigDetailComponent', () => {
  let gigServiceStub: { getGig: ReturnType<typeof vi.fn>; getMatches: ReturnType<typeof vi.fn> };
  let profileServiceStub: { getPublicTutorProfile: ReturnType<typeof vi.fn> };

  const gig: GigRequest = {
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

  const tutorProfile: TutorProfile = {
    userId: 'tutor-1',
    bio: 'Bio',
    subjects: ['Math'],
    hourlyRateMad: 100,
    verificationStatus: 'VERIFIED',
    avgRating: 4.5
  };

  function configure(gigId: string | null) {
    return TestBed.configureTestingModule({
      imports: [GigDetailComponent],
      providers: [
        provideRouter([]),
        provideHttpClient(),
        provideHttpClientTesting(),
        provideTranslateService({ fallbackLang: 'fr' }),
        { provide: GigService, useValue: gigServiceStub },
        { provide: ProfileService, useValue: profileServiceStub },
        {
          provide: ActivatedRoute,
          useValue: { snapshot: { paramMap: convertToParamMap(gigId ? { id: gigId } : {}) } }
        }
      ]
    }).compileComponents();
  }

  it('loads the gig and enriches ranked matches with tutor profiles', async () => {
    const matches: MatchSuggestion[] = [{ tutorUserId: 'tutor-1', similarityScore: 0.91 }];
    gigServiceStub = { getGig: vi.fn().mockReturnValue(of(gig)), getMatches: vi.fn().mockReturnValue(of(matches)) };
    profileServiceStub = { getPublicTutorProfile: vi.fn().mockReturnValue(of(tutorProfile)) };
    await configure('gig-1');
    const fixture = TestBed.createComponent(GigDetailComponent);
    fixture.detectChanges();

    expect(fixture.componentInstance.gig()).toEqual(gig);
    expect(fixture.componentInstance.matches()).toEqual([{ tutorUserId: 'tutor-1', similarityScore: 0.91, tutor: tutorProfile }]);
    expect(fixture.componentInstance.loading()).toBe(false);
  });

  it('shows an empty match list when there are no matches, without calling profile lookups', async () => {
    gigServiceStub = { getGig: vi.fn().mockReturnValue(of(gig)), getMatches: vi.fn().mockReturnValue(of([])) };
    profileServiceStub = { getPublicTutorProfile: vi.fn() };
    await configure('gig-1');
    const fixture = TestBed.createComponent(GigDetailComponent);
    fixture.detectChanges();

    expect(fixture.componentInstance.matches()).toEqual([]);
    expect(profileServiceStub.getPublicTutorProfile).not.toHaveBeenCalled();
  });

  it('sets the error state when loading the gig fails', async () => {
    gigServiceStub = { getGig: vi.fn().mockReturnValue(throwError(() => new Error('403'))), getMatches: vi.fn() };
    profileServiceStub = { getPublicTutorProfile: vi.fn() };
    await configure('gig-1');
    const fixture = TestBed.createComponent(GigDetailComponent);
    fixture.detectChanges();

    expect(fixture.componentInstance.error()).toBe(true);
  });

  it('sets the error state immediately when no gig id param is present', async () => {
    gigServiceStub = { getGig: vi.fn(), getMatches: vi.fn() };
    profileServiceStub = { getPublicTutorProfile: vi.fn() };
    await configure(null);
    const fixture = TestBed.createComponent(GigDetailComponent);
    fixture.detectChanges();

    expect(fixture.componentInstance.error()).toBe(true);
    expect(gigServiceStub.getGig).not.toHaveBeenCalled();
  });
});
