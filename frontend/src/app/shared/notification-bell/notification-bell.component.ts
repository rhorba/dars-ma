import { Component, computed, inject, signal } from '@angular/core';
import { DatePipe } from '@angular/common';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatBadgeModule } from '@angular/material/badge';
import { MatMenuModule } from '@angular/material/menu';
import { TranslatePipe } from '@ngx-translate/core';
import { catchError, of } from 'rxjs';
import { NotificationService } from '../../core/notification/notification.service';
import { Notification } from '../../core/notification/notification.models';

@Component({
  selector: 'app-notification-bell',
  standalone: true,
  imports: [MatButtonModule, MatIconModule, MatBadgeModule, MatMenuModule, TranslatePipe, DatePipe],
  templateUrl: './notification-bell.component.html',
  styleUrl: './notification-bell.component.scss'
})
export class NotificationBellComponent {
  private readonly notificationService = inject(NotificationService);

  readonly notifications = signal<Notification[]>([]);
  readonly loaded = signal(false);
  readonly unreadCount = computed(() => this.notifications().filter((n) => n.readAt === null).length);

  onMenuOpened(): void {
    if (this.loaded()) {
      return;
    }
    this.load();
  }

  private load(): void {
    this.notificationService
      .getNotifications()
      .pipe(catchError(() => of([])))
      .subscribe((notifications) => {
        this.notifications.set(notifications);
        this.loaded.set(true);
      });
  }

  markRead(notification: Notification): void {
    if (notification.readAt !== null) {
      return;
    }
    this.notificationService.markRead(notification.id).subscribe((updated) => {
      this.notifications.update((current) => current.map((n) => (n.id === updated.id ? updated : n)));
    });
  }
}
