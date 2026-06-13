import { Component, input } from '@angular/core';
import { badgeVariantClass, type BadgeVariant } from './badge-utils';

@Component({
  selector: 'app-badge',
  standalone: true,
  templateUrl: './badge.component.html',
})
export class BadgeComponent {
  readonly variant = input<BadgeVariant>('default');
  protected readonly classes = (): string => badgeVariantClass(this.variant());
}
