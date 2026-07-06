import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { DocType, TutorProfile, TutorProfileRequest, VerificationDocument } from './profile.models';

@Injectable({ providedIn: 'root' })
export class ProfileService {
  constructor(private http: HttpClient) {}

  getMyTutorProfile(): Observable<TutorProfile> {
    return this.http.get<TutorProfile>('/api/v1/profile/tutor/me');
  }

  upsertMyTutorProfile(request: TutorProfileRequest): Observable<TutorProfile> {
    return this.http.put<TutorProfile>('/api/v1/profile/tutor/me', request);
  }

  getPublicTutorProfile(userId: string): Observable<TutorProfile> {
    return this.http.get<TutorProfile>(`/api/v1/profile/tutor/${userId}`);
  }

  uploadVerificationDocument(docType: DocType, file: File): Observable<VerificationDocument> {
    const formData = new FormData();
    formData.append('docType', docType);
    formData.append('file', file);
    return this.http.post<VerificationDocument>('/api/v1/profile/tutor/me/verification-documents', formData);
  }
}
