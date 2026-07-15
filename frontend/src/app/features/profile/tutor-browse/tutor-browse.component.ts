import { Component, inject, signal } from '@angular/core';
import { RouterLink } from '@angular/router';
import { FormsModule } from '@angular/forms';
import { MatCardModule } from '@angular/material/card';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatButtonModule } from '@angular/material/button';
import { TranslatePipe } from '@ngx-translate/core';
import { catchError, of } from 'rxjs';
import { ProfileService } from '../../../core/profile/profile.service';
import { TutorProfile } from '../../../core/profile/profile.models';

@Component({
  selector: 'app-tutor-browse',
  standalone: true,
  imports: [RouterLink, FormsModule, MatCardModule, MatFormFieldModule, MatInputModule, MatButtonModule, TranslatePipe],
  templateUrl: './tutor-browse.component.html',
  styleUrl: './tutor-browse.component.scss'
})
export class TutorBrowseComponent {
  private readonly profileService = inject(ProfileService);

  readonly loading = signal(true);
  readonly error = signal(false);
  readonly tutors = signal<TutorProfile[]>([]);
  subjectFilter = '';

  constructor() {
    this.load();
  }

  load(): void {
    this.loading.set(true);
    this.error.set(false);
    this.profileService
      .browseTutors(this.subjectFilter || undefined)
      .pipe(catchError(() => of(null)))
      .subscribe((tutors) => {
        this.loading.set(false);
        if (tutors === null) {
          this.error.set(true);
          return;
        }
        this.tutors.set(tutors);
      });
  }
}
