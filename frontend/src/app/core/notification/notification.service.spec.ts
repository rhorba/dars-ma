import { TestBed } from '@angular/core/testing';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { provideHttpClient } from '@angular/common/http';
import { NotificationService } from './notification.service';
import { Notification } from './notification.models';

describe('NotificationService', () => {
  let service: NotificationService;
  let httpMock: HttpTestingController;

  const sampleNotification: Notification = {
    id: 'notification-1',
    type: 'BOOKING_COMPLETED',
    payload: { bookingId: 'booking-1' },
    readAt: null,
    createdAt: '2026-07-17T00:00:00Z'
  };

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [provideHttpClient(), provideHttpClientTesting()]
    });
    service = TestBed.inject(NotificationService);
    httpMock = TestBed.inject(HttpTestingController);
  });

  afterEach(() => httpMock.verify());

  it('getNotifications() GETs /api/v1/notifications', () => {
    service.getNotifications().subscribe((notifications) => expect(notifications).toEqual([sampleNotification]));
    const req = httpMock.expectOne('/api/v1/notifications');
    expect(req.request.method).toBe('GET');
    req.flush([sampleNotification]);
  });

  it('markRead() PATCHes /api/v1/notifications/:id/read', () => {
    const read = { ...sampleNotification, readAt: '2026-07-17T01:00:00Z' };
    service.markRead('notification-1').subscribe((notification) => expect(notification).toEqual(read));
    const req = httpMock.expectOne('/api/v1/notifications/notification-1/read');
    expect(req.request.method).toBe('PATCH');
    req.flush(read);
  });
});
