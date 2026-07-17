import { TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { provideHttpClientTesting } from '@angular/common/http/testing';
import { provideTranslateService } from '@ngx-translate/core';
import { vi, beforeEach } from 'vitest';
import { of } from 'rxjs';
import { NotificationBellComponent } from './notification-bell.component';
import { NotificationService } from '../../core/notification/notification.service';
import { Notification } from '../../core/notification/notification.models';

describe('NotificationBellComponent', () => {
  let notificationServiceStub: {
    getNotifications: ReturnType<typeof vi.fn>;
    markRead: ReturnType<typeof vi.fn>;
  };

  const unread: Notification = {
    id: 'notification-1',
    type: 'BOOKING_COMPLETED',
    payload: {},
    readAt: null,
    createdAt: '2026-07-17T00:00:00Z'
  };
  const read: Notification = { ...unread, id: 'notification-2', readAt: '2026-07-17T01:00:00Z' };

  beforeEach(async () => {
    notificationServiceStub = {
      getNotifications: vi.fn().mockReturnValue(of([unread, read])),
      markRead: vi.fn()
    };

    await TestBed.configureTestingModule({
      imports: [NotificationBellComponent],
      providers: [
        provideHttpClient(),
        provideHttpClientTesting(),
        provideTranslateService({ fallbackLang: 'fr' }),
        { provide: NotificationService, useValue: notificationServiceStub }
      ]
    }).compileComponents();
  });

  it('does not load notifications before the menu is opened', () => {
    const fixture = TestBed.createComponent(NotificationBellComponent);
    fixture.detectChanges();
    expect(notificationServiceStub.getNotifications).not.toHaveBeenCalled();
    expect(fixture.componentInstance.notifications()).toEqual([]);
  });

  it('loads notifications and computes unread count when the menu opens', () => {
    const fixture = TestBed.createComponent(NotificationBellComponent);
    fixture.detectChanges();
    fixture.componentInstance.onMenuOpened();
    expect(notificationServiceStub.getNotifications).toHaveBeenCalled();
    expect(fixture.componentInstance.notifications()).toEqual([unread, read]);
    expect(fixture.componentInstance.unreadCount()).toBe(1);
  });

  it('does not re-fetch on a second menu open', () => {
    const fixture = TestBed.createComponent(NotificationBellComponent);
    fixture.detectChanges();
    fixture.componentInstance.onMenuOpened();
    fixture.componentInstance.onMenuOpened();
    expect(notificationServiceStub.getNotifications).toHaveBeenCalledTimes(1);
  });

  it('markRead() updates the notification in place and lowers the unread count', () => {
    const fixture = TestBed.createComponent(NotificationBellComponent);
    fixture.detectChanges();
    fixture.componentInstance.onMenuOpened();
    const updated = { ...unread, readAt: '2026-07-17T02:00:00Z' };
    notificationServiceStub.markRead.mockReturnValue(of(updated));

    fixture.componentInstance.markRead(unread);

    expect(notificationServiceStub.markRead).toHaveBeenCalledWith('notification-1');
    expect(fixture.componentInstance.unreadCount()).toBe(0);
  });

  it('markRead() is a no-op for an already-read notification', () => {
    const fixture = TestBed.createComponent(NotificationBellComponent);
    fixture.detectChanges();
    fixture.componentInstance.onMenuOpened();
    fixture.componentInstance.markRead(read);
    expect(notificationServiceStub.markRead).not.toHaveBeenCalled();
  });
});
