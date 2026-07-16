import { TestBed } from '@angular/core/testing';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { provideHttpClient } from '@angular/common/http';
import { MessageService } from './message.service';
import { Message } from './message.models';

describe('MessageService', () => {
  let service: MessageService;
  let httpMock: HttpTestingController;

  const sampleMessage: Message = {
    id: 'message-1',
    senderId: 'student-1',
    body: 'Hello',
    createdAt: '2026-07-16T00:00:00Z'
  };

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [provideHttpClient(), provideHttpClientTesting()]
    });
    service = TestBed.inject(MessageService);
    httpMock = TestBed.inject(HttpTestingController);
  });

  afterEach(() => httpMock.verify());

  it('sendMessage() POSTs the request body to /api/v1/bookings/:id/messages', () => {
    service.sendMessage('booking-1', { body: 'Hello' }).subscribe((message) => expect(message).toEqual(sampleMessage));
    const req = httpMock.expectOne('/api/v1/bookings/booking-1/messages');
    expect(req.request.method).toBe('POST');
    expect(req.request.body).toEqual({ body: 'Hello' });
    req.flush(sampleMessage);
  });

  it('getMessages() GETs /api/v1/bookings/:id/messages', () => {
    service.getMessages('booking-1').subscribe((messages) => expect(messages).toEqual([sampleMessage]));
    const req = httpMock.expectOne('/api/v1/bookings/booking-1/messages');
    expect(req.request.method).toBe('GET');
    req.flush([sampleMessage]);
  });
});
