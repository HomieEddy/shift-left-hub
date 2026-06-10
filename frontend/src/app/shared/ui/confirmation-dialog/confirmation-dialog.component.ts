import { Component, computed, inject, signal } from '@angular/core';
import { DIALOG_DATA, DialogRef } from '@angular/cdk/dialog';
import { ConfirmationData } from './confirmation-dialog.model';
import { TranslationService } from '../../../core/i18n/translation.service';
import { from } from 'rxjs';

@Component({
  selector: 'app-confirm-dialog',
  standalone: true,
  template: `
    <div class="bg-surface-primary rounded-xl shadow-xl p-6 w-[420px]">
      <h2 class="text-lg font-semibold text-text-primary mb-2">{{ data.title }}</h2>
      <p class="text-text-secondary text-sm mb-6">{{ data.message }}</p>

      @if (errorMessage()) {
        <div class="bg-accent-danger-muted border border-accent-danger text-accent-danger text-sm rounded-lg p-3 mb-4">
          {{ errorMessage() }}
        </div>
      }

      <div class="flex justify-end gap-3">
        <button
          type="button"
          (click)="cancel()"
          [disabled]="loading()"
          class="px-4 py-2 text-sm font-medium text-text-secondary bg-surface-primary border border-border-default rounded-lg hover:bg-surface-secondary disabled:opacity-50 disabled:cursor-not-allowed"
        >
          {{ cancelLabel() }}
        </button>
        <button
          type="button"
          data-confirm-btn
          (click)="confirm()"
          [disabled]="loading()"
          class="px-4 py-2 text-sm font-medium text-white bg-primary-600 rounded-lg hover:bg-primary-700 disabled:opacity-50 disabled:cursor-not-allowed inline-flex items-center gap-2"
        >
          @if (loading()) {
            <span class="inline-block w-4 h-4 border-2 border-white/30 border-t-white rounded-full animate-spin"></span>
          }
          {{ loading() ? savingLabel() : data.confirmLabel }}
        </button>
      </div>
    </div>
  `,
})
export class ConfirmDialogComponent {
  private ts = inject(TranslationService);
  protected cancelLabel = computed(() => this.ts.translate('confirm.action.cancel'));
  protected savingLabel = computed(() => this.ts.translate('confirm.state.saving'));
  dialogRef = inject(DialogRef<boolean>);
  data: ConfirmationData = inject<ConfirmationData>(DIALOG_DATA);

  loading = signal(false);
  errorMessage = signal<string | null>(null);

  confirm(): void {
    if (this.data.onConfirm) {
      this.loading.set(true);
      this.errorMessage.set(null);

      try {
        const promise = Promise.resolve(this.data.onConfirm());
        from(promise).subscribe({
          next: () => this.dialogRef.close(true),
          error: (err: unknown) => {
            this.loading.set(false);
            const fallbackMsg = this.ts.translate('confirm.error.generic');
            this.errorMessage.set(err instanceof Error ? err.message : fallbackMsg);
          },
        });
      } catch (err) {
        this.loading.set(false);
        this.errorMessage.set(err instanceof Error ? err.message : 'Unexpected error');
      }
    } else {
      this.dialogRef.close(true);
    }
  }

  cancel(): void {
    this.dialogRef.close(false);
  }
}
