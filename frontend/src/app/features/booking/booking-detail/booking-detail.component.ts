import { Component, inject, signal } from '@angular/core';
import { ActivatedRoute } from '@angular/router';
import { MatCardModule } from '@angular/material/card';
import { MatButtonModule } from '@angular/material/button';
import { TranslatePipe } from '@ngx-translate/core';
import { catchError, of } from 'rxjs';
import { BookingService } from '../../../core/booking/booking.service';
import { AuthService } from '../../../core/auth/auth.service';
import { Booking } from '../../../core/booking/booking.models';

@Component({
  selector: 'app-booking-detail',
  standalone: true,
  imports: [MatCardModule, MatButtonModule, TranslatePipe],
  templateUrl: './booking-detail.component.html',
  styleUrl: './booking-detail.component.scss'
})
export class BookingDetailComponent {
  private readonly route = inject(ActivatedRoute);
  private readonly bookingService = inject(BookingService);
  private readonly authService = inject(AuthService);

  private readonly bookingId = this.route.snapshot.paramMap.get('id')!;

  readonly booking = signal<Booking | null>(null);
  readonly loading = signal(true);
  readonly error = signal(false);
  readonly confirming = signal(false);
  readonly disputing = signal(false);

  constructor() {
    this.load();
  }

  private load(): void {
    this.loading.set(true);
    this.error.set(false);
    this.bookingService
      .getBooking(this.bookingId)
      .pipe(catchError(() => of(null)))
      .subscribe((booking) => {
        this.loading.set(false);
        if (booking === null) {
          this.error.set(true);
          return;
        }
        this.booking.set(booking);
      });
  }

  isStudent(booking: Booking): boolean {
    return booking.studentUserId === this.authService.userId();
  }

  isTutor(booking: Booking): boolean {
    return booking.tutorUserId === this.authService.userId();
  }

  canConfirm(booking: Booking): boolean {
    if (booking.status !== 'ESCROW_HELD') {
      return false;
    }
    if (this.isStudent(booking) && !booking.studentConfirmedAt) {
      return true;
    }
    return this.isTutor(booking) && !booking.tutorConfirmedAt;
  }

  canDispute(booking: Booking): boolean {
    return booking.status === 'ESCROW_HELD' && (this.isStudent(booking) || this.isTutor(booking));
  }

  confirm(): void {
    this.confirming.set(true);
    this.bookingService.completeBooking(this.bookingId).subscribe({
      next: (booking) => {
        this.confirming.set(false);
        this.booking.set(booking);
      },
      error: () => this.confirming.set(false)
    });
  }

  dispute(): void {
    this.disputing.set(true);
    this.bookingService.disputeBooking(this.bookingId).subscribe({
      next: (booking) => {
        this.disputing.set(false);
        this.booking.set(booking);
      },
      error: () => this.disputing.set(false)
    });
  }
}
