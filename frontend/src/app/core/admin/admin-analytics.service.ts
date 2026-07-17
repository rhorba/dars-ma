import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { AdminAnalytics } from './admin.models';

@Injectable({ providedIn: 'root' })
export class AdminAnalyticsService {
  constructor(private http: HttpClient) {}

  getAnalytics(): Observable<AdminAnalytics> {
    return this.http.get<AdminAnalytics>('/api/v1/admin/analytics');
  }
}
