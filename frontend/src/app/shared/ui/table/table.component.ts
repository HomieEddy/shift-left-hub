import { Component, inject, input, output } from '@angular/core';
import { TranslationService } from '../../../core/i18n/translation.service';

export interface Column {
  key: string;
  label: string;
  sortable?: boolean;
  width?: string;
}

@Component({
  selector: 'app-table',
  standalone: true,
  template: `
    <div class="bg-white rounded-xl shadow-sm border border-slate-200 overflow-hidden">
      <table class="w-full text-sm">
        <thead class="bg-slate-50 border-b border-slate-200">
          <tr>
            @for (col of columns(); track col.key) {
              <th
                [style.width]="col.width"
                (click)="col.sortable && sortBy(col.key)"
                class="text-left px-4 py-3 font-medium text-slate-600"
                [class.cursor-pointer.hover:text-slate-800]="col.sortable"
              >
                {{ col.label }}
                @if (col.sortable && sortKey() === col.key) {
                  <span class="text-xs">
                    {{ sortDir() === 'asc' ? ' ▲' : ' ▼' }}
                  </span>
                }
              </th>
            }
          </tr>
        </thead>
        <tbody class="divide-y divide-slate-100">
          <ng-content />
        </tbody>
      </table>
      @if (empty()) {
        <div class="text-center py-8 text-slate-500">
          {{ emptyText() }}
        </div>
      }
    </div>
  `,
})
export class TableComponent {
  private translationService = inject(TranslationService);

  columns = input<Column[]>([]);
  emptyText = input(this.translationService.translate('shared.table.empty'));
  empty = input(false);
  sortKey = input<string>('');
  sortDir = input<'asc' | 'desc'>('asc');
  sort = output<string>();

  sortBy(key: string): void {
    this.sort.emit(key);
  }
}
