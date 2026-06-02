import { Component, input, output } from '@angular/core';
import { NgIf } from '@angular/common';

@Component({
  selector: 'app-modal',
  standalone: true,
  imports: [NgIf],
  template: `
    <div *ngIf="open()" class="fixed inset-0 bg-black/40 flex items-center justify-center z-50">
      <div class="bg-white rounded-xl shadow-xl p-6 w-full" [class]="width()">
        <div class="flex items-center justify-between mb-4">
          <h3 class="text-lg font-semibold text-slate-800">{{ title() }}</h3>
          <button (click)="close.emit()" class="text-slate-400 hover:text-slate-600 text-xl leading-none">&times;</button>
        </div>
        <ng-content />
      </div>
    </div>
  `,
})
export class ModalComponent {
  open = input(false);
  title = input('');
  width = input('max-w-md');
  close = output<void>();
}
