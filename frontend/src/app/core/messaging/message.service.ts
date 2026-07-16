import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { Message, MessageCreateRequest } from './message.models';

@Injectable({ providedIn: 'root' })
export class MessageService {
  constructor(private http: HttpClient) {}

  sendMessage(bookingId: string, request: MessageCreateRequest): Observable<Message> {
    return this.http.post<Message>(`/api/v1/bookings/${bookingId}/messages`, request);
  }

  getMessages(bookingId: string): Observable<Message[]> {
    return this.http.get<Message[]>(`/api/v1/bookings/${bookingId}/messages`);
  }
}
