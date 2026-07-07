import { Component, inject, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatButtonModule } from '@angular/material/button';
import { MatCardModule } from '@angular/material/card';
import { TranslatePipe } from '@ngx-translate/core';
import { GigService } from '../../../core/gig/gig.service';
import { GigRequest } from '../../../core/gig/gig.models';

@Component({
  selector: 'app-gig-create-form',
  standalone: true,
  imports: [ReactiveFormsModule, MatFormFieldModule, MatInputModule, MatButtonModule, MatCardModule, TranslatePipe],
  templateUrl: './gig-create-form.component.html',
  styleUrl: './gig-create-form.component.scss'
})
export class GigCreateFormComponent {
  private readonly fb = inject(FormBuilder);
  private readonly gigService = inject(GigService);

  readonly submitting = signal(false);
  readonly createdGig = signal<GigRequest | null>(null);
  readonly errorMessageKey = signal<string | null>(null);

  readonly form = this.fb.nonNullable.group({
    subject: ['', [Validators.required, Validators.maxLength(100)]],
    level: ['', [Validators.required, Validators.maxLength(50)]],
    description: ['', [Validators.required, Validators.maxLength(5000)]],
    budgetMinMad: this.fb.control<number | null>(null, [Validators.min(0.01)]),
    budgetMaxMad: this.fb.control<number | null>(null, [Validators.min(0.01)])
  });

  submit(): void {
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }
    const raw = this.form.getRawValue();
    if (raw.budgetMinMad !== null && raw.budgetMaxMad !== null && raw.budgetMinMad > raw.budgetMaxMad) {
      this.errorMessageKey.set('gig.create.budgetRangeError');
      return;
    }

    this.submitting.set(true);
    this.errorMessageKey.set(null);
    this.gigService.createGig(raw).subscribe({
      next: (gig) => {
        this.submitting.set(false);
        this.createdGig.set(gig);
        this.form.reset({ subject: '', level: '', description: '', budgetMinMad: null, budgetMaxMad: null });
      },
      error: () => {
        this.submitting.set(false);
        this.errorMessageKey.set('gig.create.submitError');
      }
    });
  }
}
