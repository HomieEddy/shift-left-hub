import { Component, input } from '@angular/core';
import { badgeVariantClass, type BadgeVariant } from './badge-utils';

@Component({
  selector: 'app-badge',
  standalone: true,
  template: `
    <span
      class="inline-flex items-center px-2 py-0.5 rounded-full text-xs font-medium"
      [class]="classes()"
    >
      <ng-content />
    </span>
  `,
})
export class BadgeComponent {
  readonly variant = input<BadgeVariant>('default');
  protected readonly classes = (): string => badgeVariantClass(this.variant());
}
