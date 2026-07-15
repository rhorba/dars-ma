import { Component, inject, signal } from '@angular/core';
import { MatCardModule } from '@angular/material/card';
import { MatButtonModule } from '@angular/material/button';
import { TranslatePipe } from '@ngx-translate/core';
import { AdminDisputeService, DisputeResolution } from '../../../core/admin/admin-dispute.service';
import { Booking } from '../../../core/booking/booking.models';

@Component({
  selector: 'app-dispute-queue',
  standalone: true,
  imports: [MatCardModule, MatButtonModule, TranslatePipe],
  templateUrl: './dispute-queue.component.html',
  styleUrl: './dispute-queue.component.scss'
})
export class DisputeQueueComponent {
  private readonly adminDisputeService = inject(AdminDisputeService);

  readonly bookings = signal<Booking[]>([]);
  readonly actionMessageKey = signal<string | null>(null);

  constructor() {
    this.loadQueue();
  }

  loadQueue(): void {
    this.adminDisputeService.getDisputedBookings().subscribe((bookings) => this.bookings.set(bookings));
  }

  resolve(bookingId: string, resolution: DisputeResolution): void {
    this.adminDisputeService.resolve(bookingId, resolution).subscribe({
      next: () => {
        this.actionMessageKey.set('admin.disputes.resolveSuccess');
        this.loadQueue();
      },
      error: () => this.actionMessageKey.set('admin.disputes.actionError')
    });
  }
}
