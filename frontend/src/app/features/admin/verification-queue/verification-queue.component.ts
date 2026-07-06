import { Component, inject, signal } from '@angular/core';
import { MatCardModule } from '@angular/material/card';
import { MatButtonModule } from '@angular/material/button';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { FormsModule } from '@angular/forms';
import { TranslatePipe } from '@ngx-translate/core';
import { AdminVerificationService } from '../../../core/admin/admin-verification.service';
import { VerificationQueueItem } from '../../../core/admin/admin.models';

@Component({
  selector: 'app-verification-queue',
  standalone: true,
  imports: [MatCardModule, MatButtonModule, MatFormFieldModule, MatInputModule, FormsModule, TranslatePipe],
  templateUrl: './verification-queue.component.html',
  styleUrl: './verification-queue.component.scss'
})
export class VerificationQueueComponent {
  private readonly adminVerificationService = inject(AdminVerificationService);

  readonly items = signal<VerificationQueueItem[]>([]);
  readonly rejectReasons = signal<Record<string, string>>({});
  readonly actionMessageKey = signal<string | null>(null);

  constructor() {
    this.loadQueue();
  }

  loadQueue(): void {
    this.adminVerificationService.getQueue().subscribe((items) => this.items.set(items));
  }

  reasonFor(documentId: string): string {
    return this.rejectReasons()[documentId] ?? '';
  }

  setReason(documentId: string, reason: string): void {
    this.rejectReasons.update((reasons) => ({ ...reasons, [documentId]: reason }));
  }

  approve(documentId: string): void {
    this.adminVerificationService.approve(documentId).subscribe({
      next: () => {
        this.actionMessageKey.set('admin.verification.approveSuccess');
        this.loadQueue();
      },
      error: () => this.actionMessageKey.set('admin.verification.actionError')
    });
  }

  reject(documentId: string): void {
    const reason = this.reasonFor(documentId).trim();
    if (!reason) {
      return;
    }
    this.adminVerificationService.reject(documentId, reason).subscribe({
      next: () => {
        this.actionMessageKey.set('admin.verification.rejectSuccess');
        this.loadQueue();
      },
      error: () => this.actionMessageKey.set('admin.verification.actionError')
    });
  }
}
