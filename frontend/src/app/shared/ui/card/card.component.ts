import { Component } from '@angular/core';

@Component({
  selector: 'app-card',
  standalone: true,
  template: `
    <div class="bg-white rounded-xl shadow-sm border border-slate-200 p-6">
      <ng-content />
    </div>
  `,
})
export class CardComponent {}
