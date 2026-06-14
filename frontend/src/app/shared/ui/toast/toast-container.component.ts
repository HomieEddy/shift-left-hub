import { Component, computed, DestroyRef, inject, signal } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { ToastService } from './toast.service';
import { ToastMessage, DEFAULT_DURATION } from './toast.model';
import { TranslationService } from '../../../core/i18n/translation.service';

@Component({
  selector: 'app-toast-container',
  standalone: true,
  templateUrl: './toast-container.component.html',
  styleUrl: './toast-container.component.css',
})
export class ToastContainer {
  private readonly toastService = inject(ToastService);
  private readonly destroyRef = inject(DestroyRef);
  private readonly ts = inject(TranslationService);
  protected readonly dismissLabel = computed(() => this.ts.translate('toast.dismiss'));

  protected readonly toasts = signal<ToastMessage[]>([]);

  constructor() {
    this.toastService.toasts$
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe((toast: ToastMessage) => {
        this.toasts.update((t: ToastMessage[]) => [...t, toast]);
        const duration = toast.duration ?? DEFAULT_DURATION;
        setTimeout(() => this.dismissToast(toast.id), duration);
      });

    this.toastService.dismiss$.pipe(takeUntilDestroyed(this.destroyRef)).subscribe((id: string) => {
      this.toasts.update((t: ToastMessage[]) => t.filter((toast) => toast.id !== id));
    });
  }

  protected toastClass(toast: ToastMessage): string {
    switch (toast.type) {
      case 'success':
        return 'bg-accent-success-muted border-accent-success text-accent-success';
      case 'error':
        return 'bg-accent-danger-muted border-accent-danger text-accent-danger';
      case 'warning':
        return 'bg-accent-warning-muted border-accent-warning text-accent-warning';
      case 'info':
        return 'bg-accent-info-muted border-accent-info text-accent-info';
      default:
        return 'bg-surface-secondary border-border-default text-text-primary';
    }
  }

  protected dismissToast(id: string): void {
    this.toastService.dismiss(id);
  }

  protected runAction(toast: ToastMessage): void {
    toast.action?.handler();
    this.dismissToast(toast.id);
  }
}
