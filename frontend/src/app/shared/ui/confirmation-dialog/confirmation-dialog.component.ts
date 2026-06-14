import { Component, computed, inject, signal } from '@angular/core';
import { DIALOG_DATA, DialogRef } from '@angular/cdk/dialog';
import { ConfirmationData } from './confirmation-dialog.model';
import { TranslationService } from '../../../core/i18n/translation.service';
import { from } from 'rxjs';

@Component({
  selector: 'app-confirm-dialog',
  standalone: true,
  templateUrl: './confirmation-dialog.component.html',
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
        this.errorMessage.set(
          err instanceof Error ? err.message : this.ts.translate('confirm.error.generic'),
        );
      }
    } else {
      this.dialogRef.close(true);
    }
  }

  cancel(): void {
    this.dialogRef.close(false);
  }
}
