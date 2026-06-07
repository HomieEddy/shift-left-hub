import { $localize } from '@angular/localize';
import { Component, inject, signal } from '@angular/core';
import { DIALOG_DATA, DialogRef } from '@angular/cdk/dialog';
import { NgIf } from '@angular/common';
import { ConfirmationData } from './confirmation-dialog.model';
import { from, catchError, of, switchMap } from 'rxjs';

@Component({
  selector: 'app-confirm-dialog',
  standalone: true,
  imports: [NgIf],
  template: `
    <div class="bg-white rounded-xl shadow-xl p-6 w-[420px]">
      <h2 class="text-lg font-semibold text-slate-800 mb-2">{{ data.title }}</h2>
      <p class="text-slate-600 text-sm mb-6">{{ data.message }}</p>

      <div *ngIf="errorMessage()" class="bg-red-50 border border-red-200 text-red-700 text-sm rounded-lg p-3 mb-4">
        {{ errorMessage() }}
      </div>

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
          <span *ngIf="loading()" class="inline-block w-4 h-4 border-2 border-white/30 border-t-white rounded-full animate-spin"></span>
          {{ loading() ? savingLabel : data.confirmLabel }}
        </button>
      </div>
    </div>
  `,
})
export class ConfirmDialogComponent {
  protected cancelLabel = $localize`:@@confirm.action.cancel:Cancel`;
  protected savingLabel = $localize`:@@confirm.state.saving:Saving...`;
  dialogRef = inject(DialogRef<boolean>);
  data: ConfirmationData = inject(DIALOG_DATA);

  loading = signal(false);
  errorMessage = signal<string | null>(null);

  confirm(): void {
    if (this.data.onConfirm) {
      this.loading.set(true);
      this.errorMessage.set(null);
      const result$ = from(this.data.onConfirm()).pipe(
        switchMap(() => of(true)),
        catchError((err) => {
          this.loading.set(false);
          this.errorMessage.set($localize`:@@confirm.error.generic:An unexpected error occurred. Please try again.`);
          return of(undefined);
        })
      );
      result$.subscribe((val) => {
        if (val) {
          this.dialogRef.close(true);
        }
      });
    } else {
      this.dialogRef.close(true);
    }
  }

  cancel(): void {
    this.dialogRef.close(false);
  }
}
