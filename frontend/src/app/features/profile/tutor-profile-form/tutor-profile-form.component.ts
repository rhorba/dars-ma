import { Component, inject, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatButtonModule } from '@angular/material/button';
import { MatCardModule } from '@angular/material/card';
import { MatSelectModule } from '@angular/material/select';
import { TranslatePipe } from '@ngx-translate/core';
import { catchError, of } from 'rxjs';
import { ProfileService } from '../../../core/profile/profile.service';
import { DocType, TutorProfile } from '../../../core/profile/profile.models';

@Component({
  selector: 'app-tutor-profile-form',
  standalone: true,
  imports: [
    ReactiveFormsModule,
    MatFormFieldModule,
    MatInputModule,
    MatButtonModule,
    MatCardModule,
    MatSelectModule,
    TranslatePipe
  ],
  templateUrl: './tutor-profile-form.component.html',
  styleUrl: './tutor-profile-form.component.scss'
})
export class TutorProfileFormComponent {
  private readonly fb = inject(FormBuilder);
  private readonly profileService = inject(ProfileService);

  readonly docTypes: DocType[] = ['DIPLOMA', 'CERTIFICATE', 'ID'];

  readonly profile = signal<TutorProfile | null>(null);
  readonly saving = signal(false);
  readonly saveMessageKey = signal<string | null>(null);

  readonly selectedDocType = signal<DocType>('DIPLOMA');
  readonly selectedFile = signal<File | null>(null);
  readonly uploading = signal(false);
  readonly uploadMessageKey = signal<string | null>(null);

  readonly form = this.fb.nonNullable.group({
    bio: [''],
    subjects: ['', Validators.required],
    hourlyRateMad: [0, [Validators.required, Validators.min(0.01)]]
  });

  constructor() {
    this.profileService
      .getMyTutorProfile()
      .pipe(catchError(() => of(null)))
      .subscribe((profile) => {
        if (profile) {
          this.profile.set(profile);
          this.form.setValue({
            bio: profile.bio ?? '',
            subjects: profile.subjects.join(', '),
            hourlyRateMad: profile.hourlyRateMad
          });
        }
      });
  }

  save(): void {
    if (this.form.invalid) {
      return;
    }
    this.saving.set(true);
    this.saveMessageKey.set(null);
    const raw = this.form.getRawValue();
    const subjects = raw.subjects
      .split(',')
      .map((s) => s.trim())
      .filter((s) => s.length > 0);

    this.profileService.upsertMyTutorProfile({ bio: raw.bio, subjects, hourlyRateMad: raw.hourlyRateMad }).subscribe({
      next: (profile) => {
        this.profile.set(profile);
        this.saving.set(false);
        this.saveMessageKey.set('profile.tutor.saveSuccess');
      },
      error: () => {
        this.saving.set(false);
        this.saveMessageKey.set('profile.tutor.saveError');
      }
    });
  }

  onFileSelected(event: Event): void {
    const input = event.target as HTMLInputElement;
    this.selectedFile.set(input.files?.[0] ?? null);
  }

  upload(): void {
    const file = this.selectedFile();
    if (!file) {
      return;
    }
    this.uploading.set(true);
    this.uploadMessageKey.set(null);
    this.profileService.uploadVerificationDocument(this.selectedDocType(), file).subscribe({
      next: () => {
        this.uploading.set(false);
        this.uploadMessageKey.set('profile.tutor.uploadSuccess');
        this.selectedFile.set(null);
      },
      error: () => {
        this.uploading.set(false);
        this.uploadMessageKey.set('profile.tutor.uploadError');
      }
    });
  }
}
