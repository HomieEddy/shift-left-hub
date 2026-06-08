import { Component, inject, signal } from '@angular/core';
import { DIALOG_DATA, DialogRef } from '@angular/cdk/dialog';
import { ConfirmationData } from './confirmation-dialog.model';
import { from } from 'rxjs';

@Component({
  selector: 'app-confirm-dialog',
  standalone: true,
  template: `
    <div class="bg-white rounded-xl shadow-xl p-6 w-[420px]">
      <h2 class="text-lg font-semibold text-slate-800 mb-2">{{ data.title }}</h2>
      <p class="text-slate-600 text-sm mb-6">{{ data.message }}</p>

      @if (errorMessage()) {
        <div class="bg-red-50 border border-red-200 text-red-700 text-sm rounded-lg p-3 mb-4">
          {{ errorMessage() }}
        </div>
      }

      <div class="flex justify-end gap-3">
        <button
          type="button"
          (click)="cancel()"
          [disabled]="loading()"
          class="px-4 py-2 text-sm font-medium text-slate-700 bg-white border border-slate-300 rounded-lg hover:bg-slate-50 disabled:opacity-50 disabled:cursor-not-allowed"
        >
          {{ cancelLabel }}
        </button>
        <button
          type="button"
          data-confirm-btn
          (click)="confirm()"
          [disabled]="loading()"
          class="px-4 py-2 text-sm font-medium text-white bg-blue-600 rounded-lg hover:bg-blue-700 disabled:opacity-50 disabled:cursor-not-allowed inline-flex items-center gap-2"
        >
          @if (loading()) {
            <span class="inline-block w-4 h-4 border-2 border-white/30 border-t-white rounded-full animate-spin"></span>
          }
          {{ loading() ? savingLabel : data.confirmLabel }}
        </button>
      </div>
    </div>
  `,
})
export class ConfirmDialogComponent {
  protected cancelLabel: string = $localize`:@@confirm.action.cancel:Cancel`;
  protected savingLabel: string = $localize`:@@confirm.state.saving:Saving...`;
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
            const fallbackMsg: string = $localize`:@@confirm.error.generic:An unexpected error occurred. Please try again.`;
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
