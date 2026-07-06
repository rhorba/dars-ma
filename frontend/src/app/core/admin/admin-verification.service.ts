import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { VerificationQueueItem } from './admin.models';

@Injectable({ providedIn: 'root' })
export class AdminVerificationService {
  constructor(private http: HttpClient) {}

  getQueue(): Observable<VerificationQueueItem[]> {
    return this.http.get<VerificationQueueItem[]>('/api/v1/admin/verification/queue');
  }

  approve(documentId: string): Observable<void> {
    return this.http.post<void>(`/api/v1/admin/verification/documents/${documentId}/approve`, {});
  }

  reject(documentId: string, reason: string): Observable<void> {
    return this.http.post<void>(`/api/v1/admin/verification/documents/${documentId}/reject`, { reason });
  }
}
