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
    <div class="bg-surface-primary rounded-xl shadow-sm border border-border-default overflow-hidden">
      <div class="overflow-x-auto">
        <table class="w-full text-sm">
          <thead class="sticky top-0 z-10">
            <tr class="bg-surface-secondary border-b border-border-default">
              @for (col of columns(); track col.key) {
                <th
                  [style.width]="col.width"
                  (click)="col.sortable && sortBy(col.key)"
                  class="text-left px-4 py-3 font-medium text-text-secondary select-none"
                  [class.cursor-pointer]="col.sortable"
                  [class.hover:text-text-primary]="col.sortable"
                >
                  <span class="inline-flex items-center gap-1">
                    {{ col.label }}
                    @if (col.sortable && sortKey() === col.key) {
                      <svg class="w-3.5 h-3.5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                        @if (sortDir() === 'asc') {
                          <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M5 15l7-7 7 7" />
                        } @else {
                          <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M19 9l-7 7-7-7" />
                        }
                      </svg>
                    }
                  </span>
                </th>
              }
            </tr>
          </thead>
          <tbody class="divide-y divide-border-light">
            <ng-content />
          </tbody>
        </table>
      </div>
      @if (empty()) {
        <div class="text-center py-12 px-4">
          <svg class="w-12 h-12 mx-auto text-text-tertiary mb-3" fill="none" stroke="currentColor" viewBox="0 0 24 24">
            <path stroke-linecap="round" stroke-linejoin="round" stroke-width="1.5" d="M20 13V6a2 2 0 00-2-2H6a2 2 0 00-2 2v7m16 0v5a2 2 0 01-2 2H6a2 2 0 01-2-2v-5m16 0h-2.586a1 1 0 00-.707.293l-2.414 2.414a1 1 0 01-.707.293h-3.172a1 1 0 01-.707-.293l-2.414-2.414A1 1 0 006.586 13H4" />
          </svg>
          <p class="text-text-tertiary text-sm">{{ emptyText() }}</p>
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
