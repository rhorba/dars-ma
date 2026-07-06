import { TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { provideHttpClientTesting } from '@angular/common/http/testing';
import { provideNoopAnimations } from '@angular/platform-browser/animations';
import { provideTranslateService } from '@ngx-translate/core';
import { vi } from 'vitest';
import { of, throwError } from 'rxjs';
import { TutorProfileFormComponent } from './tutor-profile-form.component';
import { ProfileService } from '../../../core/profile/profile.service';
import { TutorProfile } from '../../../core/profile/profile.models';

describe('TutorProfileFormComponent', () => {
  let profileServiceStub: {
    getMyTutorProfile: ReturnType<typeof vi.fn>;
    upsertMyTutorProfile: ReturnType<typeof vi.fn>;
    uploadVerificationDocument: ReturnType<typeof vi.fn>;
  };

  const existingProfile: TutorProfile = {
    userId: 'user-1',
    bio: 'Bio',
    subjects: ['Math', 'Physics'],
    hourlyRateMad: 150,
    verificationStatus: 'PENDING',
    avgRating: null
  };

  beforeEach(async () => {
    profileServiceStub = {
      getMyTutorProfile: vi.fn().mockReturnValue(of(existingProfile)),
      upsertMyTutorProfile: vi.fn(),
      uploadVerificationDocument: vi.fn()
    };

    await TestBed.configureTestingModule({
      imports: [TutorProfileFormComponent],
      providers: [
        provideHttpClient(),
        provideHttpClientTesting(),
        provideNoopAnimations(),
        provideTranslateService({ fallbackLang: 'fr' }),
        { provide: ProfileService, useValue: profileServiceStub }
      ]
    }).compileComponents();
  });

  it('loads and populates the existing profile on init', () => {
    const fixture = TestBed.createComponent(TutorProfileFormComponent);
    fixture.detectChanges();

    expect(fixture.componentInstance.profile()).toEqual(existingProfile);
    expect(fixture.componentInstance.form.getRawValue()).toEqual({
      bio: 'Bio',
      subjects: 'Math, Physics',
      hourlyRateMad: 150
    });
  });

  it('leaves the form untouched when no profile exists yet (404)', () => {
    profileServiceStub.getMyTutorProfile.mockReturnValue(throwError(() => new Error('not found')));
    const fixture = TestBed.createComponent(TutorProfileFormComponent);
    fixture.detectChanges();

    expect(fixture.componentInstance.profile()).toBeNull();
  });

  it('does not save when the form is invalid', () => {
    const fixture = TestBed.createComponent(TutorProfileFormComponent);
    fixture.detectChanges();
    fixture.componentInstance.form.patchValue({ subjects: '' });

    fixture.componentInstance.save();

    expect(profileServiceStub.upsertMyTutorProfile).not.toHaveBeenCalled();
  });

  it('save() splits the comma-separated subjects and calls upsert', () => {
    profileServiceStub.upsertMyTutorProfile.mockReturnValue(of(existingProfile));
    const fixture = TestBed.createComponent(TutorProfileFormComponent);
    fixture.detectChanges();
    fixture.componentInstance.form.setValue({ bio: 'New bio', subjects: 'Math,  Chemistry ', hourlyRateMad: 200 });

    fixture.componentInstance.save();

    expect(profileServiceStub.upsertMyTutorProfile).toHaveBeenCalledWith({
      bio: 'New bio',
      subjects: ['Math', 'Chemistry'],
      hourlyRateMad: 200
    });
    expect(fixture.componentInstance.saveMessageKey()).toBe('profile.tutor.saveSuccess');
  });

  it('save() sets an error message when the request fails', () => {
    profileServiceStub.upsertMyTutorProfile.mockReturnValue(throwError(() => new Error('conflict')));
    const fixture = TestBed.createComponent(TutorProfileFormComponent);
    fixture.detectChanges();

    fixture.componentInstance.save();

    expect(fixture.componentInstance.saveMessageKey()).toBe('profile.tutor.saveError');
  });

  it('upload() does nothing when no file is selected', () => {
    const fixture = TestBed.createComponent(TutorProfileFormComponent);
    fixture.detectChanges();

    fixture.componentInstance.upload();

    expect(profileServiceStub.uploadVerificationDocument).not.toHaveBeenCalled();
  });

  it('upload() sends the selected file and clears it on success', () => {
    profileServiceStub.uploadVerificationDocument.mockReturnValue(
      of({ id: 'doc-1', docType: 'DIPLOMA', originalFilename: 'd.pdf', reviewedAt: null, createdAt: '2026-01-01T00:00:00Z' })
    );
    const fixture = TestBed.createComponent(TutorProfileFormComponent);
    fixture.detectChanges();
    const file = new File(['content'], 'd.pdf', { type: 'application/pdf' });
    fixture.componentInstance.selectedFile.set(file);

    fixture.componentInstance.upload();

    expect(profileServiceStub.uploadVerificationDocument).toHaveBeenCalledWith('DIPLOMA', file);
    expect(fixture.componentInstance.uploadMessageKey()).toBe('profile.tutor.uploadSuccess');
    expect(fixture.componentInstance.selectedFile()).toBeNull();
  });

  it('upload() sets an error message when the request fails', () => {
    profileServiceStub.uploadVerificationDocument.mockReturnValue(throwError(() => new Error('bad type')));
    const fixture = TestBed.createComponent(TutorProfileFormComponent);
    fixture.detectChanges();
    fixture.componentInstance.selectedFile.set(new File(['x'], 'x.exe'));

    fixture.componentInstance.upload();

    expect(fixture.componentInstance.uploadMessageKey()).toBe('profile.tutor.uploadError');
  });
});
