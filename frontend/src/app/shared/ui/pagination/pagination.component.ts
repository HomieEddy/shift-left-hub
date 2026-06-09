import { Component, inject, input, output } from '@angular/core';
import { TranslationService } from '../../../core/i18n/translation.service';

@Component({
  selector: 'app-pagination',
  standalone: true,
  template: `
    @if (totalPages() > 1) {
      <div class="flex items-center justify-center gap-2 mt-6">
      <button
        (click)="goToPage(currentPage() - 1)"
        [disabled]="currentPage() === 0"
        class="px-3 py-1.5 text-sm rounded border border-slate-300 text-slate-600 hover:bg-slate-50 disabled:opacity-50 disabled:cursor-not-allowed"

      >
        {{ translationService.translate('pagination.previous') }}
      </button>
      <span class="text-sm text-slate-600">
        {{ translationService.translate('pagination.pageOf') }} {{ currentPage() + 1 }} of {{ totalPages() }}
      </span>
      <button
        (click)="goToPage(currentPage() + 1)"
        [disabled]="currentPage() >= totalPages() - 1"
        class="px-3 py-1.5 text-sm rounded border border-slate-300 text-slate-600 hover:bg-slate-50 disabled:opacity-50 disabled:cursor-not-allowed"

      >
        {{ translationService.translate('pagination.next') }}
      </button>
      </div>
    }
  `,
})
export class PaginationComponent {
  protected translationService = inject(TranslationService);

  currentPage = input(0);
  totalPages = input(0);
  pageChange = output<number>();

  goToPage(page: number): void {
    if (page >= 0 && page < this.totalPages()) {
      this.pageChange.emit(page);
    }
  }
}
