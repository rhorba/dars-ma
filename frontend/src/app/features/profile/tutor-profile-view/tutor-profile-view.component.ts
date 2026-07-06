import { Component, inject, signal } from '@angular/core';
import { ActivatedRoute } from '@angular/router';
import { MatCardModule } from '@angular/material/card';
import { TranslatePipe } from '@ngx-translate/core';
import { catchError, of } from 'rxjs';
import { ProfileService } from '../../../core/profile/profile.service';
import { TutorProfile } from '../../../core/profile/profile.models';

@Component({
  selector: 'app-tutor-profile-view',
  standalone: true,
  imports: [MatCardModule, TranslatePipe],
  templateUrl: './tutor-profile-view.component.html',
  styleUrl: './tutor-profile-view.component.scss'
})
export class TutorProfileViewComponent {
  private readonly route = inject(ActivatedRoute);
  private readonly profileService = inject(ProfileService);

  readonly profile = signal<TutorProfile | null>(null);
  readonly notFound = signal(false);

  constructor() {
    const userId = this.route.snapshot.paramMap.get('userId');
    if (!userId) {
      this.notFound.set(true);
      return;
    }
    this.profileService
      .getPublicTutorProfile(userId)
      .pipe(catchError(() => of(null)))
      .subscribe((profile) => {
        if (profile) {
          this.profile.set(profile);
        } else {
          this.notFound.set(true);
        }
      });
  }
}
