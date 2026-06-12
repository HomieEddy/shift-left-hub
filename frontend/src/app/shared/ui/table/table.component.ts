import { Component, input, output } from '@angular/core';

export interface Column {
  key: string;
  label: string;
  sortable?: boolean;
  width?: string;
}

@Component({
  selector: 'app-table',
  standalone: true,
  templateUrl: './table.component.html',
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
