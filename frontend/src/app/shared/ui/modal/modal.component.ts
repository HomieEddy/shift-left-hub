import { Component, HostListener, input, output } from '@angular/core';

@Component({
  selector: 'app-modal',
  standalone: true,
  template: `
    @if (open()) {
      <div
        class="fixed inset-0 z-50 flex items-center justify-center"
        role="dialog"
        aria-modal="true"
        [attr.aria-label]="title()"
      >
        <button
          class="fixed inset-0 bg-black/50 backdrop-blur-sm animate-fade-in"
          (click)="closed.emit()"
          tabindex="-1"
          [attr.aria-label]="'Close ' + title()"
        ></button>
        <div
          class="relative bg-surface-primary rounded-xl shadow-2xl w-full animate-scale-in"
          [class]="width()"
        >
          <div class="flex items-center justify-between px-6 py-4 border-b border-border-light">
            <h3 class="text-lg font-semibold text-text-primary">{{ title() }}</h3>
            <button
              (click)="closed.emit()"
              class="text-text-tertiary hover:text-text-primary transition-colors p-1 rounded-md hover:bg-surface-secondary"
              [attr.aria-label]="'Close ' + title()"
            >
              <svg class="w-5 h-5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M6 18L18 6M6 6l12 12" />
              </svg>
            </button>
          </div>
          <div class="p-6">
            <ng-content />
          </div>
        </div>
      </div>
    }
  `,
  styles: `
    @keyframes fadeIn {
      from { opacity: 0; }
      to { opacity: 1; }
    }
    @keyframes scaleIn {
      from { opacity: 0; transform: scale(0.95); }
      to { opacity: 1; transform: scale(1); }
    }
    .animate-fade-in {
      animation: fadeIn 150ms ease-out;
    }
    .animate-scale-in {
      animation: scaleIn 150ms ease-out;
    }
    @media (prefers-reduced-motion: reduce) {
      .animate-fade-in,
      .animate-scale-in {
        animation: none;
      }
    }
  `,
})
export class ModalComponent {
  open = input(false);
  title = input('');
  width = input('max-w-md');
  closed = output<void>();

  @HostListener('document:keydown.escape')
  onEscape(): void {
    if (this.open()) {
      this.closed.emit();
    }
  }
}
