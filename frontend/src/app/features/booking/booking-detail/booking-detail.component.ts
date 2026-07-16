import { Component, inject, signal } from '@angular/core';
import { ActivatedRoute } from '@angular/router';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { MatCardModule } from '@angular/material/card';
import { MatButtonModule } from '@angular/material/button';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatSelectModule } from '@angular/material/select';
import { MatInputModule } from '@angular/material/input';
import { TranslatePipe } from '@ngx-translate/core';
import { catchError, of } from 'rxjs';
import { BookingService } from '../../../core/booking/booking.service';
import { AuthService } from '../../../core/auth/auth.service';
import { Booking } from '../../../core/booking/booking.models';
import { ReviewService } from '../../../core/review/review.service';
import { Review } from '../../../core/review/review.models';
import { MessageService } from '../../../core/messaging/message.service';
import { Message } from '../../../core/messaging/message.models';

@Component({
  selector: 'app-booking-detail',
  standalone: true,
  imports: [
    MatCardModule,
    MatButtonModule,
    MatFormFieldModule,
    MatSelectModule,
    MatInputModule,
    ReactiveFormsModule,
    TranslatePipe
  ],
  templateUrl: './booking-detail.component.html',
  styleUrl: './booking-detail.component.scss'
})
export class BookingDetailComponent {
  private readonly route = inject(ActivatedRoute);
  private readonly fb = inject(FormBuilder);
  private readonly bookingService = inject(BookingService);
  private readonly authService = inject(AuthService);
  private readonly reviewService = inject(ReviewService);
  private readonly messageService = inject(MessageService);

  private readonly bookingId = this.route.snapshot.paramMap.get('id')!;

  readonly booking = signal<Booking | null>(null);
  readonly loading = signal(true);
  readonly error = signal(false);
  readonly confirming = signal(false);
  readonly disputing = signal(false);

  readonly reviews = signal<Review[]>([]);
  readonly submittingReview = signal(false);
  readonly reviewError = signal(false);

  readonly reviewForm = this.fb.nonNullable.group({
    rating: [5, [Validators.required, Validators.min(1), Validators.max(5)]],
    comment: ['']
  });

  readonly ratingOptions = [1, 2, 3, 4, 5];

  readonly messages = signal<Message[]>([]);
  readonly sendingMessage = signal(false);

  readonly messageForm = this.fb.nonNullable.group({
    body: ['', [Validators.required]]
  });

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
        if (booking.status === 'COMPLETED') {
          this.loadReviews();
        }
        this.loadMessages();
      });
  }

  private loadReviews(): void {
    this.reviewService
      .getReviews(this.bookingId)
      .pipe(catchError(() => of([])))
      .subscribe((reviews) => this.reviews.set(reviews));
  }

  private loadMessages(): void {
    this.messageService
      .getMessages(this.bookingId)
      .pipe(catchError(() => of([])))
      .subscribe((messages) => this.messages.set(messages));
  }

  sendMessage(): void {
    if (this.messageForm.invalid) {
      this.messageForm.markAllAsTouched();
      return;
    }
    this.sendingMessage.set(true);
    const { body } = this.messageForm.getRawValue();
    this.messageService.sendMessage(this.bookingId, { body }).subscribe({
      next: (message) => {
        this.sendingMessage.set(false);
        this.messages.update((current) => [...current, message]);
        this.messageForm.reset({ body: '' });
      },
      error: () => this.sendingMessage.set(false)
    });
  }

  authUserId(): string | null {
    return this.authService.userId();
  }

  hasReviewed(): boolean {
    const userId = this.authService.userId();
    return this.reviews().some((r) => r.reviewerId === userId);
  }

  submitReview(): void {
    if (this.reviewForm.invalid) {
      this.reviewForm.markAllAsTouched();
      return;
    }
    this.submittingReview.set(true);
    this.reviewError.set(false);
    const { rating, comment } = this.reviewForm.getRawValue();
    this.reviewService.submitReview(this.bookingId, { rating, comment: comment || null }).subscribe({
      next: (review) => {
        this.submittingReview.set(false);
        this.reviews.update((current) => [...current, review]);
      },
      error: () => {
        this.submittingReview.set(false);
        this.reviewError.set(true);
      }
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
