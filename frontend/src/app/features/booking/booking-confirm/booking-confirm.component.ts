import { Component, inject, signal } from '@angular/core';
import { ActivatedRoute, Router } from '@angular/router';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatButtonModule } from '@angular/material/button';
import { MatCardModule } from '@angular/material/card';
import { TranslatePipe } from '@ngx-translate/core';
import { catchError, of } from 'rxjs';
import { BookingService } from '../../../core/booking/booking.service';
import { ProfileService } from '../../../core/profile/profile.service';
import { TutorProfile } from '../../../core/profile/profile.models';

@Component({
  selector: 'app-booking-confirm',
  standalone: true,
  imports: [ReactiveFormsModule, MatFormFieldModule, MatInputModule, MatButtonModule, MatCardModule, TranslatePipe],
  templateUrl: './booking-confirm.component.html',
  styleUrl: './booking-confirm.component.scss'
})
export class BookingConfirmComponent {
  private readonly route = inject(ActivatedRoute);
  private readonly router = inject(Router);
  private readonly fb = inject(FormBuilder);
  private readonly bookingService = inject(BookingService);
  private readonly profileService = inject(ProfileService);

  private readonly gigId = this.route.snapshot.paramMap.get('gigId')!;
  private readonly tutorUserId = this.route.snapshot.paramMap.get('tutorUserId')!;

  readonly tutor = signal<TutorProfile | null>(null);
  readonly loading = signal(true);
  readonly submitting = signal(false);
  readonly errorMessageKey = signal<string | null>(null);

  readonly form = this.fb.nonNullable.group({
    durationHours: [1, [Validators.required, Validators.min(0.5), Validators.max(12)]]
  });

  constructor() {
    this.profileService
      .getPublicTutorProfile(this.tutorUserId)
      .pipe(catchError(() => of(null)))
      .subscribe((tutor) => {
        this.loading.set(false);
        this.tutor.set(tutor);
      });
  }

  estimatedPrice(): number | null {
    const tutor = this.tutor();
    const duration = this.form.controls.durationHours.value;
    return tutor && duration ? tutor.hourlyRateMad * duration : null;
  }

  submit(): void {
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }
    this.submitting.set(true);
    this.errorMessageKey.set(null);
    this.bookingService
      .createBooking({
        gigRequestId: this.gigId,
        tutorUserId: this.tutorUserId,
        durationHours: this.form.getRawValue().durationHours
      })
      .subscribe({
        next: (booking) => {
          this.submitting.set(false);
          this.router.navigate(['/bookings', booking.id]);
        },
        error: () => {
          this.submitting.set(false);
          this.errorMessageKey.set('booking.confirm.submitError');
        }
      });
  }
}
