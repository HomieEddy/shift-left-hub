import { Component, computed, DestroyRef, inject, signal } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { ToastService } from './toast.service';
import { ToastMessage } from './toast.model';
import { TranslationService } from '../../../core/i18n/translation.service';

@Component({
  selector: 'app-toast-container',
  standalone: true,
  template: `
    <div class="fixed top-4 right-4 z-[9999] flex flex-col gap-2 w-80 pointer-events-none" aria-live="polite" role="status">
      @for (toast of toasts(); track toast.id) {
        <div
          class="pointer-events-auto flex items-start gap-3 p-3 rounded-lg border shadow-lg animate-slide-in-right"
          [class]="toastClass(toast)"
        >
          <span class="shrink-0 mt-0.5">
            @switch (toast.type) {
              @case ('success') {
                <svg class="w-5 h-5 text-accent-success" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="M22 11.08V12a10 10 0 1 1-5.93-9.14"/><polyline points="22 4 12 14.01 9 11.01"/></svg>
              }
              @case ('error') {
                <svg class="w-5 h-5 text-accent-danger" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><circle cx="12" cy="12" r="10"/><line x1="15" y1="9" x2="9" y2="15"/><line x1="9" y1="9" x2="15" y2="15"/></svg>
              }
              @case ('warning') {
                <svg class="w-5 h-5 text-accent-warning" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="M10.29 3.86L1.82 18a2 2 0 0 0 1.71 3h16.94a2 2 0 0 0 1.71-3L13.71 3.86a2 2 0 0 0-3.42 0z"/><line x1="12" y1="9" x2="12" y2="13"/><line x1="12" y1="17" x2="12.01" y2="17"/></svg>
              }
              @case ('info') {
                <svg class="w-5 h-5 text-accent-info" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><circle cx="12" cy="12" r="10"/><line x1="12" y1="16" x2="12" y2="12"/><line x1="12" y1="8" x2="12.01" y2="8"/></svg>
              }
              @default {
                <svg class="w-5 h-5 text-text-secondary" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><circle cx="12" cy="12" r="10"/><line x1="12" y1="16" x2="12" y2="12"/><line x1="12" y1="8" x2="12.01" y2="8"/></svg>
              }
            }
          </span>
          <p class="text-sm flex-1 mr-2 break-words">{{ toast.message }}</p>
          <div class="flex items-center gap-1 shrink-0">
            @if (toast.action) {
              <button
                (click)="runAction(toast)"
                class="text-sm font-medium underline underline-offset-2 hover:opacity-80"
              >
                {{ toast.action.label }}
              </button>
            }
            <button
              (click)="dismissToast(toast.id)"
              class="p-0.5 rounded hover:bg-black/5 transition-colors"
              [attr.aria-label]="dismissLabel()"
            >
              <svg class="w-4 h-4" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><line x1="18" y1="6" x2="6" y2="18"/><line x1="6" y1="6" x2="18" y2="18"/></svg>
            </button>
          </div>
        </div>
      }
    </div>
  `,
  styles: `
    @keyframes slide-in-right {
      from { transform: translateX(100%); opacity: 0; }
      to { transform: translateX(0); opacity: 1; }
    }
    .animate-slide-in-right {
      animation: slide-in-right 0.25s ease-out;
    }
  `,
})
export class ToastContainer {
  private readonly toastService = inject(ToastService);
  private readonly destroyRef = inject(DestroyRef);
  private readonly ts = inject(TranslationService);
  protected readonly dismissLabel = computed(() => this.ts.translate('toast.dismiss'));

  protected readonly toasts = signal<ToastMessage[]>([]);

  constructor() {
    this.toastService.toasts$.pipe(takeUntilDestroyed(this.destroyRef)).subscribe((toast: ToastMessage) => {
      this.toasts.update((t: ToastMessage[]) => [...t, toast]);
      const duration = toast.duration ?? 5000;
      setTimeout(() => this.dismissToast(toast.id), duration);
    });

    this.toastService.dismiss$.pipe(takeUntilDestroyed(this.destroyRef)).subscribe((id: string) => {
      this.toasts.update((t: ToastMessage[]) => t.filter(toast => toast.id !== id));
    });
  }

  protected toastClass(toast: ToastMessage): string {
    switch (toast.type) {
      case 'success': return 'bg-accent-success-muted border-accent-success text-accent-success';
      case 'error': return 'bg-accent-danger-muted border-accent-danger text-accent-danger';
      case 'warning': return 'bg-accent-warning-muted border-accent-warning text-accent-warning';
      case 'info': return 'bg-accent-info-muted border-accent-info text-accent-info';
      default: return 'bg-surface-secondary border-border-default text-text-primary';
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
