import { Component, input } from '@angular/core';

@Component({
  selector: 'app-card',
  standalone: true,
  template: `
    <div
      class="bg-surface-primary rounded-xl shadow-sm border border-border-default"
      [class.p-6]="padding() === 'default'"
      [class.p-4]="padding() === 'sm'"
      [class.p-0]="padding() === 'none'"
    >
      <ng-content />
    </div>
  `,
})
export class CardComponent {
  padding = input<'default' | 'sm' | 'none'>('default');
}
