import { Component, input, output } from '@angular/core';
import { NgFor, NgIf } from '@angular/common';

export interface Column {
  key: string;
  label: string;
  sortable?: boolean;
  width?: string;
}

@Component({
  selector: 'app-table',
  standalone: true,
  imports: [NgFor, NgIf],
  template: `
    <div class="bg-white rounded-xl shadow-sm border border-slate-200 overflow-hidden">
      <table class="w-full text-sm">
        <thead class="bg-slate-50 border-b border-slate-200">
          <tr>
            <th
              *ngFor="let col of columns()"
              [style.width]="col.width"
              (click)="col.sortable && sortBy(col.key)"
              class="text-left px-4 py-3 font-medium text-slate-600"
              [class.cursor-pointer.hover:text-slate-800]="col.sortable"
            >
              {{ col.label }}
              <span *ngIf="col.sortable && sortKey() === col.key" class="text-xs">
                {{ sortDir() === 'asc' ? ' ▲' : ' ▼' }}
              </span>
            </th>
          </tr>
        </thead>
        <tbody class="divide-y divide-slate-100">
          <ng-content />
        </tbody>
      </table>
      <div *ngIf="empty()" class="text-center py-8 text-slate-500">
        {{ emptyText() }}
      </div>
    </div>
  `,
})
export class TableComponent {
  columns = input<Column[]>([]);
  emptyText = input('No data found.');
  empty = input(false);
  sortKey = input<string>('');
  sortDir = input<'asc' | 'desc'>('asc');
  sort = output<string>();

  sortBy(key: string): void {
    this.sort.emit(key);
  }
}
