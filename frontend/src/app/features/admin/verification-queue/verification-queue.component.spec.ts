import { TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { provideHttpClientTesting } from '@angular/common/http/testing';
import { provideNoopAnimations } from '@angular/platform-browser/animations';
import { provideTranslateService } from '@ngx-translate/core';
import { vi } from 'vitest';
import { of, throwError } from 'rxjs';
import { VerificationQueueComponent } from './verification-queue.component';
import { AdminVerificationService } from '../../../core/admin/admin-verification.service';
import { VerificationQueueItem } from '../../../core/admin/admin.models';

describe('VerificationQueueComponent', () => {
  let adminServiceStub: {
    getQueue: ReturnType<typeof vi.fn>;
    approve: ReturnType<typeof vi.fn>;
    reject: ReturnType<typeof vi.fn>;
  };

  const item: VerificationQueueItem = {
    documentId: 'doc-1',
    tutorUserId: 'user-1',
    tutorFullName: 'Karim',
    docType: 'DIPLOMA',
    originalFilename: 'diploma.pdf',
    submittedAt: '2026-01-01T00:00:00Z'
  };

  beforeEach(async () => {
    adminServiceStub = {
      getQueue: vi.fn().mockReturnValue(of([item])),
      approve: vi.fn(),
      reject: vi.fn()
    };

    await TestBed.configureTestingModule({
      imports: [VerificationQueueComponent],
      providers: [
        provideHttpClient(),
        provideHttpClientTesting(),
        provideNoopAnimations(),
        provideTranslateService({ fallbackLang: 'fr' }),
        { provide: AdminVerificationService, useValue: adminServiceStub }
      ]
    }).compileComponents();
  });

  it('loads the queue on init', () => {
    const fixture = TestBed.createComponent(VerificationQueueComponent);
    fixture.detectChanges();

    expect(fixture.componentInstance.items()).toEqual([item]);
  });

  it('approve() reloads the queue and sets a success message', () => {
    adminServiceStub.approve.mockReturnValue(of(undefined));
    const fixture = TestBed.createComponent(VerificationQueueComponent);
    fixture.detectChanges();

    fixture.componentInstance.approve('doc-1');

    expect(adminServiceStub.approve).toHaveBeenCalledWith('doc-1');
    expect(fixture.componentInstance.actionMessageKey()).toBe('admin.verification.approveSuccess');
    expect(adminServiceStub.getQueue).toHaveBeenCalledTimes(2);
  });

  it('reject() does nothing when no reason is set', () => {
    const fixture = TestBed.createComponent(VerificationQueueComponent);
    fixture.detectChanges();

    fixture.componentInstance.reject('doc-1');

    expect(adminServiceStub.reject).not.toHaveBeenCalled();
  });

  it('reject() sends the trimmed reason and reloads on success', () => {
    adminServiceStub.reject.mockReturnValue(of(undefined));
    const fixture = TestBed.createComponent(VerificationQueueComponent);
    fixture.detectChanges();
    fixture.componentInstance.setReason('doc-1', '  blurry  ');

    fixture.componentInstance.reject('doc-1');

    expect(adminServiceStub.reject).toHaveBeenCalledWith('doc-1', 'blurry');
    expect(fixture.componentInstance.actionMessageKey()).toBe('admin.verification.rejectSuccess');
  });

  it('approve() sets an error message when the request fails', () => {
    adminServiceStub.approve.mockReturnValue(throwError(() => new Error('conflict')));
    const fixture = TestBed.createComponent(VerificationQueueComponent);
    fixture.detectChanges();

    fixture.componentInstance.approve('doc-1');

    expect(fixture.componentInstance.actionMessageKey()).toBe('admin.verification.actionError');
  });
});
