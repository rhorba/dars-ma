import { Component, inject, signal } from '@angular/core';
import { DecimalPipe } from '@angular/common';
import { MatCardModule } from '@angular/material/card';
import { TranslatePipe } from '@ngx-translate/core';
import { catchError, of } from 'rxjs';
import { AdminAnalyticsService } from '../../../core/admin/admin-analytics.service';
import { AdminAnalytics } from '../../../core/admin/admin.models';

@Component({
  selector: 'app-analytics-dashboard',
  standalone: true,
  imports: [MatCardModule, TranslatePipe, DecimalPipe],
  templateUrl: './analytics-dashboard.component.html',
  styleUrl: './analytics-dashboard.component.scss'
})
export class AnalyticsDashboardComponent {
  private readonly adminAnalyticsService = inject(AdminAnalyticsService);

  readonly analytics = signal<AdminAnalytics | null>(null);
  readonly loading = signal(true);
  readonly error = signal(false);

  constructor() {
    this.load();
  }

  private load(): void {
    this.loading.set(true);
    this.error.set(false);
    this.adminAnalyticsService
      .getAnalytics()
      .pipe(catchError(() => of(null)))
      .subscribe((analytics) => {
        this.loading.set(false);
        if (analytics === null) {
          this.error.set(true);
          return;
        }
        this.analytics.set(analytics);
      });
  }

  totalSignups(analytics: AdminAnalytics): number {
    return analytics.studentSignups + analytics.tutorSignups;
  }
}
