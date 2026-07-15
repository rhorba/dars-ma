import { Component, inject, signal } from '@angular/core';
import { ActivatedRoute, RouterLink } from '@angular/router';
import { MatCardModule } from '@angular/material/card';
import { MatButtonModule } from '@angular/material/button';
import { TranslatePipe } from '@ngx-translate/core';
import { catchError, forkJoin, map, of, switchMap } from 'rxjs';
import { GigService } from '../../../core/gig/gig.service';
import { ProfileService } from '../../../core/profile/profile.service';
import { GigRequest } from '../../../core/gig/gig.models';
import { TutorProfile } from '../../../core/profile/profile.models';

export interface RankedTutorMatch {
  tutorUserId: string;
  similarityScore: number;
  tutor: TutorProfile | null;
}

@Component({
  selector: 'app-gig-detail',
  standalone: true,
  imports: [RouterLink, MatCardModule, MatButtonModule, TranslatePipe],
  templateUrl: './gig-detail.component.html',
  styleUrl: './gig-detail.component.scss'
})
export class GigDetailComponent {
  private readonly route = inject(ActivatedRoute);
  private readonly gigService = inject(GigService);
  private readonly profileService = inject(ProfileService);

  readonly gig = signal<GigRequest | null>(null);
  readonly matches = signal<RankedTutorMatch[]>([]);
  readonly loading = signal(true);
  readonly error = signal(false);

  private gigId: string | null = null;

  constructor() {
    this.gigId = this.route.snapshot.paramMap.get('id');
    if (!this.gigId) {
      this.loading.set(false);
      this.error.set(true);
      return;
    }
    this.load(this.gigId);
  }

  retry(): void {
    if (this.gigId) {
      this.load(this.gigId);
    }
  }

  private load(gigId: string): void {
    this.loading.set(true);
    this.error.set(false);
    this.gigService
      .getGig(gigId)
      .pipe(
        switchMap((gig) =>
          this.gigService.getMatches(gigId).pipe(
            switchMap((suggestions) =>
              suggestions.length === 0
                ? of([])
                : forkJoin(
                    suggestions.map((suggestion) =>
                      this.profileService.getPublicTutorProfile(suggestion.tutorUserId).pipe(
                        map((tutor): RankedTutorMatch => ({ ...suggestion, tutor })),
                        catchError(() => of<RankedTutorMatch>({ ...suggestion, tutor: null }))
                      )
                    )
                  )
            ),
            map((matches) => ({ gig, matches }))
          )
        ),
        catchError(() => of(null))
      )
      .subscribe((result) => {
        this.loading.set(false);
        if (result === null) {
          this.error.set(true);
          return;
        }
        this.gig.set(result.gig);
        this.matches.set(result.matches);
      });
  }
}
