import { Component, DestroyRef, inject, input, output, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { Subject, debounceTime, distinctUntilChanged } from 'rxjs';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { TranslationService } from '../../../core/i18n/translation.service';

@Component({
  selector: 'app-search-input',
  standalone: true,
  imports: [FormsModule],
  template: `
    <div class="relative">
      <svg class="absolute left-3 top-2.5 w-4 h-4 text-text-tertiary pointer-events-none" fill="none" stroke="currentColor" viewBox="0 0 24 24">
        <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M21 21l-6-6m2-5a7 7 0 11-14 0 7 7 0 0114 0z" />
      </svg>
      <input
        type="text"
        [attr.aria-label]="placeholder()"
        [placeholder]="placeholder()"
        i18n-placeholder="@@shared.search.placeholder"
        [ngModel]="internalValue()"
        (ngModelChange)="onInput($event)"
        class="w-full pl-10 pr-10 py-2 bg-surface-primary border border-border-default rounded-lg text-sm text-text-primary placeholder:text-text-tertiary focus:outline-none focus:ring-2 focus:ring-primary-500 focus:border-transparent"
      />
      @if (loading()) {
        <svg class="absolute right-3 top-2.5 w-4 h-4 text-text-tertiary animate-spin" fill="none" viewBox="0 0 24 24">
          <circle class="opacity-25" cx="12" cy="12" r="10" stroke="currentColor" stroke-width="4" />
          <path class="opacity-75" fill="currentColor" d="M4 12a8 8 0 018-8V0C5.373 0 0 5.373 0 12h4z" />
        </svg>
      } @else if (internalValue()) {
        <button
          (click)="clear()"
          class="absolute right-3 top-2.5 text-text-tertiary hover:text-text-primary transition-colors"
          [attr.aria-label]="'Clear search'"
        >
          <svg class="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
            <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M6 18L18 6M6 6l12 12" />
          </svg>
        </button>
      }
    </div>
  `,
})
export class SearchInputComponent {
  protected translationService = inject(TranslationService);
  placeholder = input('Search...');
  debounceMs = input(300);
  query = input('');
  loading = input(false);
  searchChange = output<string>();

  internalValue = signal('');
  private searchSubject = new Subject<string>();
  private destroyRef = inject(DestroyRef);

  constructor() {
    this.searchSubject.pipe(
      debounceTime(this.debounceMs()),
      distinctUntilChanged(),
      takeUntilDestroyed(this.destroyRef),
    ).subscribe(value => this.searchChange.emit(value));
  }

  onInput(value: string): void {
    this.internalValue.set(value);
    this.searchSubject.next(value);
  }

  clear(): void {
    this.internalValue.set('');
    this.searchSubject.next('');
  }
}
