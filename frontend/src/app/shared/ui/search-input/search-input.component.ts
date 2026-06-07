import { Component, input, output, signal, effect } from '@angular/core';
import { FormsModule } from '@angular/forms';

@Component({
  selector: 'app-search-input',
  standalone: true,
  imports: [FormsModule],
  template: `
    <div class="relative">
      <input
        type="text"
        [attr.aria-label]="placeholder()"
        [placeholder]="placeholder()"
        i18n-placeholder="@@shared.search.placeholder"
        [ngModel]="query()"
        (ngModelChange)="onInput($event)"
        class="w-full pl-10 pr-4 py-2 border border-slate-300 rounded-lg focus:outline-none focus:ring-2 focus:ring-blue-500 focus:border-transparent text-sm"
      />
      <svg class="absolute left-3 top-2.5 w-4 h-4 text-slate-400" fill="none" stroke="currentColor" viewBox="0 0 24 24">
        <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M21 21l-6-6m2-5a7 7 0 11-14 0 7 7 0 0114 0z" />
      </svg>
    </div>
  `,
})
export class SearchInputComponent {
  placeholder = input('Search...');
  debounceMs = input(300);
  query = input('');
  search = output<string>();

  private debounceTimer: ReturnType<typeof setTimeout> | null = null;

  onInput(value: string): void {
    if (this.debounceTimer) clearTimeout(this.debounceTimer);
    this.debounceTimer = setTimeout(() => {
      this.search.emit(value);
    }, this.debounceMs());
  }
}
