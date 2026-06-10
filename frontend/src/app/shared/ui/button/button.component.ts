import { Component, input, output } from '@angular/core';

type ButtonVariant = 'primary' | 'secondary' | 'outline' | 'ghost' | 'danger';
type ButtonSize = 'sm' | 'md' | 'lg';

@Component({
  selector: 'app-button',
  standalone: true,
  template: `
    <button
      [type]="type()"
      [disabled]="disabled() || loading()"
      (click)="clicked.emit()"
      [attr.aria-disabled]="disabled() || loading()"
      [class]="buttonClasses()"
    >
      @if (loading()) {
        <span class="inline-block w-4 h-4 border-2 border-current/30 border-t-current rounded-full animate-spin"></span>
      }
      <ng-content />
    </button>
  `,
})
export class ButtonComponent {
  readonly variant = input<ButtonVariant>('primary');
  readonly size = input<ButtonSize>('md');
  readonly type = input<'button' | 'submit' | 'reset'>('button');
  readonly disabled = input(false);
  readonly loading = input(false);
  readonly fullWidth = input(false);
  readonly clicked = output<void>();

  protected buttonClasses(): string {
    const base = 'inline-flex items-center justify-center gap-2 font-medium rounded-lg transition-colors focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-primary-500';
    const width = this.fullWidth() ? ' w-full' : '';
    const disabledStyles = ' disabled:opacity-50 disabled:cursor-not-allowed';

    const variants: Record<string, string> = {
      primary: 'bg-primary-600 text-white hover:bg-primary-700',
      secondary: 'bg-surface-tertiary text-text-primary hover:bg-surface-secondary',
      outline: 'border border-border-default text-text-primary hover:bg-surface-secondary',
      ghost: 'text-text-secondary hover:bg-surface-tertiary',
      danger: 'bg-accent-danger text-white hover:bg-accent-danger/90',
    };

    const sizes: Record<string, string> = {
      sm: 'px-3 py-1.5 text-sm',
      md: 'px-4 py-2 text-sm',
      lg: 'px-5 py-2.5 text-base',
    };

    return `${base}${width}${disabledStyles} ${variants[this.variant()]} ${sizes[this.size()]}`;
  }
}

@Component({
  selector: 'app-icon-button',
  standalone: true,
  template: `
    <button
      [type]="type()"
      [disabled]="disabled() || loading()"
      (click)="clicked.emit()"
      [attr.aria-label]="ariaLabel()"
      [attr.aria-disabled]="disabled() || loading()"
      [class]="iconButtonClasses()"
    >
      @if (loading()) {
        <span class="inline-block w-4 h-4 border-2 border-current/30 border-t-current rounded-full animate-spin"></span>
      } @else {
        <ng-content />
      }
    </button>
  `,
})
export class IconButtonComponent {
  readonly variant = input<ButtonVariant>('primary');
  readonly size = input<ButtonSize>('md');
  readonly type = input<'button' | 'submit' | 'reset'>('button');
  readonly disabled = input(false);
  readonly loading = input(false);
  readonly ariaLabel = input.required<string>();
  readonly clicked = output<void>();

  protected iconButtonClasses(): string {
    const base = 'inline-flex items-center justify-center rounded-lg transition-colors focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-primary-500';
    const disabledStyles = ' disabled:opacity-50 disabled:cursor-not-allowed';

    const variants: Record<string, string> = {
      primary: 'bg-primary-600 text-white hover:bg-primary-700',
      secondary: 'bg-surface-tertiary text-text-primary hover:bg-surface-secondary',
      outline: 'border border-border-default text-text-primary hover:bg-surface-secondary',
      ghost: 'text-text-secondary hover:bg-surface-tertiary',
      danger: 'bg-accent-danger text-white hover:bg-accent-danger/90',
    };

    const sizes: Record<string, string> = {
      sm: 'p-1.5',
      md: 'p-2',
      lg: 'p-2.5',
    };

    return `${base}${disabledStyles} ${variants[this.variant()]} ${sizes[this.size()]}`;
  }
}
