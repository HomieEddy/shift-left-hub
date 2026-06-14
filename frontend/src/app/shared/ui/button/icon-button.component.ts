import { Component, input, output } from '@angular/core';

type ButtonVariant = 'primary' | 'secondary' | 'outline' | 'ghost' | 'danger';
type ButtonSize = 'sm' | 'md' | 'lg';

@Component({
  selector: 'app-icon-button',
  standalone: true,
  templateUrl: './icon-button.component.html',
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
    const base =
      'inline-flex items-center justify-center rounded-lg transition-colors focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-primary-500';
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
