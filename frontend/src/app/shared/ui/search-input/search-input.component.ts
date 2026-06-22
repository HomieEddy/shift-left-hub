import { Component, DestroyRef, inject, input, output, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { Subject, debounceTime, distinctUntilChanged } from 'rxjs';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { TranslationService } from '../../../core/i18n/translation.service';

@Component({
  selector: 'app-search-input',
  standalone: true,
  imports: [FormsModule],
  templateUrl: './search-input.component.html',
})
export class SearchInputComponent {
  protected translationService = inject(TranslationService);
  placeholder = input('Search...');
  /** Debounce delay in ms. NOTE: read once at construction time; dynamic changes after init are not reflected. */
  debounceMs = input(300);
  query = input('');
  loading = input(false);
  searchChange = output<string>();

  internalValue = signal('');
  private searchSubject = new Subject<string>();
  private destroyRef = inject(DestroyRef);

  constructor() {
    this.searchSubject
      .pipe(
        debounceTime(this.debounceMs()),
        distinctUntilChanged(),
        takeUntilDestroyed(this.destroyRef),
      )
      .subscribe((value) => this.searchChange.emit(value));
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
