import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { GigRequest, GigRequestCreateRequest } from './gig.models';

@Injectable({ providedIn: 'root' })
export class GigService {
  constructor(private http: HttpClient) {}

  createGig(request: GigRequestCreateRequest): Observable<GigRequest> {
    return this.http.post<GigRequest>('/api/v1/gigs', request);
  }

  getGig(id: string): Observable<GigRequest> {
    return this.http.get<GigRequest>(`/api/v1/gigs/${id}`);
  }
}
