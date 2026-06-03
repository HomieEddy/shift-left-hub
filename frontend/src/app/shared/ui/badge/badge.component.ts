import { Component, input } from '@angular/core';
import { NgClass } from '@angular/common';

@Component({
  selector: 'app-badge',
  standalone: true,
  imports: [NgClass],
  template: `
    <span
      class="inline-block px-2 py-0.5 rounded-full text-xs font-medium"
      [ngClass]="variant() === 'success' ? 'bg-green-100 text-green-700' :
               variant() === 'warning' ? 'bg-yellow-100 text-yellow-700' :
               variant() === 'danger' ? 'bg-red-100 text-red-700' :
               variant() === 'info' ? 'bg-blue-100 text-blue-700' :
               'bg-slate-100 text-slate-700'"
    >
      <ng-content />
    </span>
  `,
})
export class BadgeComponent {
  variant = input<'success' | 'warning' | 'danger' | 'info' | 'default'>('default');
}
