import { TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { provideHttpClientTesting } from '@angular/common/http/testing';
import { provideNoopAnimations } from '@angular/platform-browser/animations';
import { provideTranslateService } from '@ngx-translate/core';
import { vi } from 'vitest';
import { of, throwError } from 'rxjs';
import { AnalyticsDashboardComponent } from './analytics-dashboard.component';
import { AdminAnalyticsService } from '../../../core/admin/admin-analytics.service';
import { AdminAnalytics } from '../../../core/admin/admin.models';

describe('AnalyticsDashboardComponent', () => {
  let adminAnalyticsServiceStub: { getAnalytics: ReturnType<typeof vi.fn> };

  const analytics: AdminAnalytics = {
    studentSignups: 10,
    tutorSignups: 4,
    totalBookings: 20,
    completedBookings: 12,
    gmvMad: 2400,
    matchRatePercent: 80
  };

  async function configure() {
    await TestBed.configureTestingModule({
      imports: [AnalyticsDashboardComponent],
      providers: [
        provideHttpClient(),
        provideHttpClientTesting(),
        provideNoopAnimations(),
        provideTranslateService({ fallbackLang: 'fr' }),
        { provide: AdminAnalyticsService, useValue: adminAnalyticsServiceStub }
      ]
    }).compileComponents();
  }

  it('loads and displays the analytics on init', async () => {
    adminAnalyticsServiceStub = { getAnalytics: vi.fn().mockReturnValue(of(analytics)) };
    await configure();
    const fixture = TestBed.createComponent(AnalyticsDashboardComponent);
    fixture.detectChanges();

    expect(fixture.componentInstance.analytics()).toEqual(analytics);
    expect(fixture.componentInstance.loading()).toBe(false);
    expect(fixture.componentInstance.error()).toBe(false);
  });

  it('totalSignups() sums student and tutor signups', async () => {
    adminAnalyticsServiceStub = { getAnalytics: vi.fn().mockReturnValue(of(analytics)) };
    await configure();
    const fixture = TestBed.createComponent(AnalyticsDashboardComponent);
    fixture.detectChanges();

    expect(fixture.componentInstance.totalSignups(analytics)).toBe(14);
  });

  it('sets an error state when the request fails', async () => {
    adminAnalyticsServiceStub = { getAnalytics: vi.fn().mockReturnValue(throwError(() => new Error('forbidden'))) };
    await configure();
    const fixture = TestBed.createComponent(AnalyticsDashboardComponent);
    fixture.detectChanges();

    expect(fixture.componentInstance.error()).toBe(true);
    expect(fixture.componentInstance.loading()).toBe(false);
  });
});
