import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { Notification } from './notification.models';

@Injectable({ providedIn: 'root' })
export class NotificationService {
  constructor(private http: HttpClient) {}

  getNotifications(): Observable<Notification[]> {
    return this.http.get<Notification[]>('/api/v1/notifications');
  }

  markRead(notificationId: string): Observable<Notification> {
    return this.http.patch<Notification>(`/api/v1/notifications/${notificationId}/read`, {});
  }
}
